package com.xxuz.piclane.smartpower.sk

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortTimeoutException
import com.xxuz.piclane.smartpower.sk.command.ScanMode
import com.xxuz.piclane.smartpower.sk.command.SendToSecurity
import com.xxuz.piclane.smartpower.sk.event.*
import com.xxuz.piclane.smartpower.utils.CrLfPrintWriter
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.EOFException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class SkStack(device: String) {
    companion object {
        private val logger = LoggerFactory.getLogger(SkStack::class.java)

        fun addShutdownHook(thread: Thread) {
            SerialPort.addShutdownHook(thread)
        }
    }

    private val serialPort: SerialPort

    private val reader: BufferedReader

    private val writer: PrintWriter

    init {
        this.serialPort = SerialPort.getCommPort(device)

        serialPort.allowElevatedPermissionsRequest()
        serialPort.openPort().also {
            if(!it) {
                throw SkException("Error code was " + serialPort.lastErrorCode + " at Line " + serialPort.lastErrorLocation)
            }
        }
        serialPort.baudRate = 115200
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0)

        this.reader = BufferedReader(InputStreamReader(serialPort.inputStream, StandardCharsets.US_ASCII))
        this.writer = CrLfPrintWriter(OutputStreamWriter(serialPort.outputStream, StandardCharsets.US_ASCII))

        // バッファをフラッシュする
        while(true) {
            try {
                readNextLine(1000)
            } catch (e: SkException) {
                break
            }
        }
    }

    fun close() {
        reader.close()
        writer.close()
        serialPort.closePort()
    }

    private var currentLine: String? = null

    private fun readNextLine(timeout: Long = 0): Line {
        currentLine?.also { line ->
            currentLine = null
            return Line(line)
        }

        val start = System.currentTimeMillis()
        var line: String
        while(true) {
            try {
                val r = reader.readLine() ?: throw SkEofException("")
                line = r.trimEnd()
            } catch(e: SerialPortTimeoutException) {
                logger.trace("timeout")
                if(timeout != 0L && System.currentTimeMillis() - start > timeout) {
                    throw SkTimeoutException("")
                }
                continue
            }
            if(line.isNotEmpty()) {
                logger.debug(line)
                if (line[0] != 'S') {
                    return Line(line)
                }
            }
        }
    }

    private fun readNextEvent(timeout: Long = 0): EventBase {
        val line = readNextLine(timeout)
        return when(line.components[0]) {
            "EVENT" -> createEvent(line)
            "EPANDESC" -> createPanDesc()
            "ERXUDP" -> RxUdp.from(line.components)
            "OK" -> Ok
            else -> throw SkException("UNKNOWN RESPONSE", line.value)
        }
    }

    private fun createEvent(line: Line): Event {
        val components = line.components
        val num = Event.Num.valueOf(parseUInt8(components[1])) ?: throw IllegalArgumentException("")
        val sender = parseUInt8Array(components[2].replace(":", ""))
        val param: Int? = if(components.size == 4) parseUInt8(components[3]) else null
        return Event(num, sender, param)
    }

    private fun createPanDesc(): PanDesc {
        val map = mutableMapOf<String, String>()
        while(true) {
            val line = readNextLine()
            if(!line.value.startsWith(" ")) {
                line.reject()
                break
            }
            val e = line.value.trimStart().split(":")
            map[e[0]] = e[1]
        }
        return PanDesc(
            channel = parseUInt8(map["Channel"] ?: throw IllegalArgumentException("")),
            channelPane = parseUInt8(map["Channel Page"] ?: throw IllegalArgumentException("")),
            panId = parseUInt16(map["Pan ID"] ?: throw IllegalArgumentException("")),
            addr = parseUInt8Array(map["Addr"] ?: throw IllegalArgumentException("")),
            lqi = parseUInt8(map["LQI"] ?: throw IllegalArgumentException("")),
            pairId = map["PairID"]
        )
    }

    private inner class Line(val value: String) {
        val components = value.split(" ")
        val isNotOk get() = value != "OK"

        fun reject() {
            currentLine = value
        }

        override fun toString() = value
    }

    fun version(): String {
        writer.println("SKVER")
        val version = readNextLine().let {
            it.components[1]
        }
        readNextLine().also {
            if(it.isNotOk) {
                throw SkException("SKVER の実行に失敗しました", it.value)
            }
        }
        return version
    }

    fun setPassword(password: String) {
        writer.println("SKSETPWD ${password.length.toString(16)} $password")
        readNextLine().also {
            if(it.isNotOk) {
                throw SkException("SKSETPWD の実行に失敗しました", it.value)
            }
        }
    }

    fun setRouteBId(id: String) {
        writer.println("SKSETRBID $id")
        readNextLine().also {
            if(it.isNotOk) {
                throw SkException("SKSETRBID の実行に失敗しました", it.value)
            }
        }
    }

    fun activeScan(channelMask: Long, duration: Int): List<PanDesc> {
        writer.println("SKSCAN ${toUInt8(ScanMode.ACTIVE_SCAN_WITH_IE.value)} ${toUInt32(channelMask)} ${toUInt8(duration)}")
        readNextLine().also {
            if(it.isNotOk) {
                throw SkException("SKSCAN の実行に失敗しました", it.value)
            }
        }

        val pans = mutableListOf<PanDesc>()
        while(true) {
            val event = readNextEvent()
            if(event is Event && event.num == Event.Num.ACTIVE_SCAN_COMPLETED) {
                break
            }
            if(event is PanDesc) {
                pans.add(event)
            }
        }
        return pans
    }

    fun setRegister(sreg: String, value: String) {
        writer.println("SKSREG $sreg $value")
        readNextLine().also {
            if(it.isNotOk) {
                throw SkException("SKSREG の実行に失敗しました", it.value)
            }
        }
    }

    fun ll64(mac: ByteBuffer): IpAddr {
        writer.println("SKLL64 ${toUInt8Array(mac)}")
        return IpAddr(readNextLine().value)
    }

    fun join(ipaddr: IpAddr): List<RxUdp> {
        writer.println("SKJOIN $ipaddr")
        readNextLine().also {
            if(it.isNotOk) {
                throw SkException("SKJOIN の実行に失敗しました", it.value)
            }
        }
        val rxudps = mutableListOf<RxUdp>()
        while(true) {
            val event = readNextEvent()
            if(event is Event) {
                when(event.num) {
                    Event.Num.PANA_CONNECTION_ERROR -> throw SkJoinException("PANA 接続失敗")
                    Event.Num.PANA_CONNECTION_COMPLETED -> break
                    else -> continue
                }
            } else if(event is RxUdp) {
                rxudps.add(event)
            }
        }
        return rxudps
    }

    fun sendTo(handle: Int, ipaddr: IpAddr, port: Int, sec: SendToSecurity, data: ByteBuffer) {
        val dataBytes = data.array()
        val cmd = "SKSENDTO $handle $ipaddr ${toUInt16(port)} ${toUInt8(sec.value)} ${toUInt16(dataBytes.size)} "
        val cmdBytes = cmd.toByteArray(StandardCharsets.US_ASCII)
        val crlf = "\r\n".toByteArray(StandardCharsets.US_ASCII)
        serialPort.writeBytes(cmdBytes, cmdBytes.size.toLong())
        serialPort.writeBytes(dataBytes, dataBytes.size.toLong())
        serialPort.writeBytes(crlf, crlf.size.toLong())
        while(true) {
            val event = readNextEvent()
            if (event is Ok) {
                return
            }
        }
    }

    fun waitForRxUdp(timeout: Long = 0): RxUdp {
        while(true) {
            val event = readNextEvent(timeout)
            if (event is RxUdp) {
                return event
            }
        }
    }
}
