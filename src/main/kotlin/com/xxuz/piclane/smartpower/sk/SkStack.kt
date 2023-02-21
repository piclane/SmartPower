package com.xxuz.piclane.smartpower.sk

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortTimeoutException
import com.xxuz.piclane.smartpower.sk.command.ScanMode
import com.xxuz.piclane.smartpower.sk.command.SendToSecurity
import com.xxuz.piclane.smartpower.sk.event.*
import com.xxuz.piclane.smartpower.utils.CrLfPrintWriter
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class SkStack(device: String) {
    companion object {
        /** ロガー */
        private val logger = LoggerFactory.getLogger(SkStack::class.java)

        @JvmStatic
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
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0)

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

    private var currentLine: Line? = null

    private fun readNextLine(timeout: Long = 0): Line {
        currentLine?.also { line ->
            currentLine = null
            return line
        }

        val start = System.currentTimeMillis()
        var line: String
        while(true) {
            try {
                line = reader.readLine() ?: throw SkEofException()
            } catch(e: SerialPortTimeoutException) {
                logger.trace("timeout")
                if(timeout != 0L && System.currentTimeMillis() - start > timeout) {
                    throw SkTimeoutException()
                }
                continue
            }

            line = line.trimEnd()
            if(line.isEmpty()) {
                continue
            }

            logger.debug(line)
            if (line[0] == 'S') {
                continue
            }

            return Line(line)
        }
    }

    private fun readNextEvent(timeout: Long = 0): EventBase {
        val line = readNextLine(timeout)
        return when(line.components[0]) {
            "EVENT" -> createEvent(line)
            "EPANDESC" -> createPanDesc()
            "ERXUDP" -> RxUdp.from(line.components)
            "OK" -> Ok
            "FAIL" -> Fail.from(line.components)
            else -> Unknown(line.components)
        }
    }

    private fun createEvent(line: Line): Event {
        val components = line.components
        if(components.size < 3) {
            throw SkIllegalResponseException("EVENT の引数が少なすぎます", line.value)
        }
        val num = Event.Num.valueOf(parseUInt8(components[1])) ?: throw SkIllegalResponseException("EVENT の引数 num が不正な値です", line.value)
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
            channel = parseUInt8(map["Channel"] ?: throw SkIllegalResponseException("EPANDESC の値 Channel が取得できませんでした")),
            channelPane = parseUInt8(map["Channel Page"] ?: throw SkIllegalResponseException("EPANDESC の値 Channel Page が取得できませんでした")),
            panId = parseUInt16(map["Pan ID"] ?: throw SkIllegalResponseException("EPANDESC の値 Pan ID が取得できませんでした")),
            addr = parseUInt8Array(map["Addr"] ?: throw SkIllegalResponseException("EPANDESC の値 Addr が取得できませんでした")),
            lqi = parseUInt8(map["LQI"] ?: throw SkIllegalResponseException("EPANDESC の値 LQI が取得できませんでした")),
            pairId = map["PairID"]
        )
    }

    private inner class Line(val value: String) {
        val components = value.split(" ")

        fun reject() {
            currentLine = this@Line
        }

        override fun toString() = value
    }

    fun version(): String {
        writer.println("SKVER")
        val version = readNextLine().let {
            it.components[1]
        }
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKVER の実行に失敗しました", it)
            }
        }
        return version
    }

    fun setPassword(password: String) {
        writer.println("SKSETPWD ${password.length.toString(16)} $password")
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKSETPWD の実行に失敗しました", it)
            }
        }
    }

    fun setRouteBId(id: String) {
        writer.println("SKSETRBID $id")
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKSETRBID の実行に失敗しました", it)
            }
        }
    }

    fun activeScan(channelMask: Long, duration: Int): List<PanDesc> {
        writer.println("SKSCAN ${toUInt8(ScanMode.ACTIVE_SCAN_WITH_IE.value)} ${toUInt32(channelMask)} ${toUInt8(duration)}")
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKSCAN の実行に失敗しました", it)
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
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKSREG の実行に失敗しました", it)
            }
        }
    }

    fun ll64(mac: ByteBuffer): IpAddr {
        writer.println("SKLL64 ${toUInt8Array(mac)}")
        return IpAddr(readNextLine().value)
    }

    fun join(ipaddr: IpAddr): List<RxUdp> {
        writer.println("SKJOIN $ipaddr")
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKJOIN の実行に失敗しました", it)
            }
        }
        val rxudps = mutableListOf<RxUdp>()
        while(true) {
            val event = readNextEvent()
            if(event is Event) {
                when(event.num) {
                    Event.Num.PANA_CONNECTION_ERROR -> throw SkJoinException()
                    Event.Num.PANA_CONNECTION_COMPLETED -> break
                    else -> continue
                }
            } else if(event is RxUdp) {
                rxudps.add(event)
            }
        }
        return rxudps
    }

    fun sendTo(handle: Int, ipaddr: IpAddr, port: Int, sec: SendToSecurity, dataBytes: ByteArray) {
        val cmd = "SKSENDTO $handle $ipaddr ${toUInt16(port)} ${toUInt8(sec.value)} ${toUInt16(dataBytes.size)} "
        val cmdBytes = cmd.toByteArray(StandardCharsets.US_ASCII)
        val crlfBytes = "\r\n".toByteArray(StandardCharsets.US_ASCII)
        serialPort.writeBytes(cmdBytes, cmdBytes.size.toLong())
        serialPort.writeBytes(dataBytes, dataBytes.size.toLong())
        serialPort.writeBytes(crlfBytes, crlfBytes.size.toLong())
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
