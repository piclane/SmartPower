package com.xxuz.piclane.smartpower.sk

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortTimeoutException
import com.xxuz.piclane.smartpower.sk.command.ScanMode
import com.xxuz.piclane.smartpower.sk.command.SendToSecurity
import com.xxuz.piclane.smartpower.sk.event.*
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * SKSTACK-IP の API ラッパー
 *
 * @param device デバイスへのパス
 */
@Suppress("UNUSED")
class SkStack(device: String) {
    companion object {
        /** ロガー */
        private val logger = LoggerFactory.getLogger(SkStack::class.java)

        /**
         * シリアルポート使用停止直前に呼び出されるシャットダウンフックを登録します
         */
        @JvmStatic
        fun addShutdownHook(thread: Thread) {
            SerialPort.addShutdownHook(thread)
        }
    }

    /** SerialPort */
    private val serialPort: SerialPort

    /** 読み込みバッファ */
    private val reader: BufferedReader

    /** 書き込みライター */
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

    /**
     * 利用を終了します
     */
    fun close() {
        reader.close()
        writer.close()
        serialPort.closePort()
    }

    /** 現在取得済みの行 */
    private var currentLine: Line? = null

    /**
     * バッファから1行取得します
     *
     * @param timeout 受信のタイムアウト (ミリ秒)
     */
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

    /**
     * 次のイベントを取得します
     *
     * @param timeout 受信のタイムアウト (ミリ秒)
     */
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

    /**
     * 行から Event を生成します
     */
    private fun createEvent(line: Line): Event {
        val components = line.components
        if(components.size < 3) {
            throw SkIllegalResponseException("EVENT の引数が少なすぎます", line.value)
        }
        val num = Event.Num.valueOf(parseUInt8(components[1])) ?: throw SkIllegalResponseException("EVENT の引数 num が不正な値です", line.value)
        val sender = IpAddr(components[2])
        val param: Int? = if(components.size == 4) parseUInt8(components[3]) else null
        return Event(num, sender, param)
    }

    /**
     * PanDesc を生成します
     */
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

    /**
     * 一行を表現します
     *
     * @param value 行の文字列
     */
    private inner class Line(val value: String) {
        /** 空白で区切られた値 */
        val components = value.split(" ")

        /**
         * この行をバッファに戻します
         */
        fun reject() {
            currentLine = this@Line
        }

        override fun toString() = value
    }

    /**
     * 改行を CRLF にする PrintWriter
     */
    private class CrLfPrintWriter(out: Writer) : PrintWriter(out) {
        override fun println() {
            synchronized(this.lock) {
                write("\r\n")
                out.flush()
            }
        }
    }

    /**
     * SKSTACK IP のファームウェアバージョンを取得します
     */
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

    /**
     * 指定したパスワードから PSK を生成して登録します
     */
    fun setPassword(password: String) {
        writer.println("SKSETPWD ${password.length.toString(16)} $password")
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKSETPWD の実行に失敗しました", it)
            }
        }
    }

    /**
     * 指定された ID から各 Route-B ID を生成して設定します
     */
    fun setRouteBId(id: String) {
        writer.println("SKSETRBID $id")
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKSETRBID の実行に失敗しました", it)
            }
        }
    }

    /**
     * 指定したチャンネルに対してアクティブスキャンを実行します
     *
     * @param channelMask スキャンするチャンネルをビットマップフラグで指定します。 最下位ビットがチャンネル 33 に対応します。
     * @param duration 各チャンネルのスキャン時間を指定します。
     *                 スキャン時間は以下の式で計算されます。
     *                 0.01 sec * (2^<DURATION> + 1)
     *                 値域:0-14 (6 以上を推奨)
     */
    fun activeScan(channelMask: Long, duration: Int): List<PanDesc> {
        if(duration < 0 || 14 < duration) {
            throw IllegalArgumentException("duration は 0-14 の範囲で指定する必要があります: $duration")
        }

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

    /**
     * 仮想レジスタの内容を設定します
     *
     * @param sreg アルファベット‘S’で始まるレジスタ番号を16進数で指定 します。
     * @param value レジスタに設定する値。設定値域はレジスタ番号に依存します。
     */
    fun setRegister(sreg: String, value: String) {
        writer.println("SKSREG $sreg $value")
        readNextEvent().also {
            if(it !is Ok) {
                throw SkException("SKSREG の実行に失敗しました", it)
            }
        }
    }

    /**
     * 64 ビット MAC アドレスを IPv6 リンクローカルアドレスに変換します
     *
     * @param mac 64 ビット MAC アドレス
     */
    fun ll64(mac: ByteBuffer): IpAddr {
        writer.println("SKLL64 ${toUInt8Array(mac)}")
        return IpAddr(readNextLine().value)
    }

    /**
     * 指定したIPアドレスに対して PaC (PANA認証クライアント) として PANA 接続シーケンスを開始します。
     * 実行前に PSK、PWD、Route-B ID 等のセキュリティ設定を施しておく必要があります。
     * 接続先は SKSTART コマンドでPAAとして動作開始している必要があります。
     * 接続の結果はイベントで通知されます。
     * PANA 接続シーケンスはPaCがPAAに対してのみ開始できます。
     *
     * 接続元 (PaC):
     * 接続が完了すると、指定したIPアドレスに対するセキュリティ設定が有効になり、以後の通信でデータが暗号化されます。
     *
     * 接続先 (PAA):
     * 接続先はコーディネータとして動作開始している必要があります。
     * PSKから生成した暗号キーを自動的に配布します。
     * 相手からの接続が完了すると接続元に対するセキュリティ設定が有効になり、以後の通信でデータが暗号化されます。
     * 1つのデバイスとの接続が成立すると、他デバイスからの新規の接続を受け付けなくなります。
     *
     * @param ipaddr IPアドレス
     */
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

    /**
     * 指定した宛先に UDP でデータを送信します
     * 相手側に伝わる送信元ポート番号は handle で指定した UDP ハンドルの待受ポート番号となります。
     * 未使用のハンドルを指定すると ER10 になります。
     *
     * @param handle UDP ハンドルの待受ポート番号
     * @param ipaddr 宛先 IPv6 アドレス
     * @param sec 暗号化オプション
     * @param data 送信データ (ASCII)
     */
    fun sendTo(handle: Int, ipaddr: IpAddr, port: Int, sec: SendToSecurity, data: String) {
        sendTo(handle, ipaddr, port, sec, data.toByteArray(StandardCharsets.US_ASCII))
    }

    /**
     * 指定した宛先に UDP でデータを送信します
     * 相手側に伝わる送信元ポート番号は handle で指定した UDP ハンドルの待受ポート番号となります。
     * 未使用のハンドルを指定すると ER10 になります。
     *
     * @param handle UDP ハンドルの待受ポート番号
     * @param ipaddr 宛先 IPv6 アドレス
     * @param sec 暗号化オプション
     * @param dataBytes 送信データ (バイナリ)
     */
    fun sendTo(handle: Int, ipaddr: IpAddr, port: Int, sec: SendToSecurity, dataBytes: ByteArray) {
        val cmd = "SKSENDTO $handle $ipaddr ${toUInt16(port)} ${toUInt8(sec.value)} ${toUInt16(dataBytes.size)} "
        val cmdBytes = cmd.toByteArray(StandardCharsets.US_ASCII)
        val crlfBytes = "\r\n".toByteArray(StandardCharsets.US_ASCII)
        serialPort.writeBytes(cmdBytes, cmdBytes.size.toLong())
        serialPort.writeBytes(dataBytes, dataBytes.size.toLong())
        serialPort.writeBytes(crlfBytes, crlfBytes.size.toLong())
        while(true) {
            when(val event = readNextEvent()) {
                is Ok -> return
                is Fail -> throw SkException("SKSENDTO コマンドの実行に失敗しました", event)
            }
        }
    }

    /**
     * UDP (マルチキャスト含む) を受信するまで待機します
     *
     * @param timeout イベント受信のタイムアウト (ミリ秒)
     */
    fun waitForRxUdp(timeout: Long = 0): RxUdp {
        while(true) {
            val event = readNextEvent(timeout)
            if (event is RxUdp) {
                return event
            }
        }
    }
}
