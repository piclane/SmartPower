package com.xxuz.piclane.smartpower.sk.event

import com.xxuz.piclane.smartpower.sk.IpAddr
import com.xxuz.piclane.smartpower.sk.parseBoolean
import com.xxuz.piclane.smartpower.sk.parseUInt16
import com.xxuz.piclane.smartpower.sk.parseUInt8Array
import com.xxuz.piclane.smartpower.sk.toUInt8Array
import java.nio.ByteBuffer

/**
 * 自端末宛ての UDP(マルチキャスト含む)を受信すると通知されます。
 */
data class RxUdp(
    /** 送信元 IPv6 アドレス */
    val sender: IpAddr,
    /** 送信先 IPv6 アドレス */
    val dest: IpAddr,
    /** 送信元ポート番号 uint16 */
    val rPort: Int,
    /** 送信先ポート番号 uint16 */
    val lPort: Int,
    /** 送信元の MAC 層アドレス(64bit) uint8[8] */
    val senderLla: ByteBuffer,
    /** 受信した IP パケットを構成する MAC フレームが暗号 化されていた場合 true そうでない場合 false (boolean) */
    val secured: Boolean,
    /** 受信したデータの長さ uint16 */
    val dataLen: Int,
    /** 受信データ 可変長 uint8[] (hex string) */
    val data: String
): EventBase {
    companion object {
        fun from(components: List<String>) = RxUdp(
            sender = IpAddr(components[1]),
            dest = IpAddr(components[2]),
            rPort = parseUInt16(components[3]),
            lPort = parseUInt16(components[4]),
            senderLla = parseUInt8Array(components[5]),
            secured = parseBoolean(components[6]),
            dataLen = parseUInt16(components[7]),
            data = components[8],
        )
    }

    override fun toString(): String {
        // ヘッダー類は 16 進表記 (0x0000) で表示
        return buildString {
            append("RxUdp(")
            append("sender=").append(sender).append(", ")
            append("dest=").append(dest).append(", ")
            append("rPort=").append(rPort.hex16()).append(", ")
            append("lPort=").append(lPort.hex16()).append(", ")
            append("senderLla=").append(toUInt8Array(senderLla)).append(", ")
            append("secured=").append(secured).append(", ")
            append("dataLen=").append(dataLen.hex16()).append(", ")
            append("data=").append(data)
            append(")")
        }
    }
}

// --- private helpers for hex formatting ---
private fun Int.hex16(): String = "0x" + this.toString(16).uppercase().padStart(4, '0')
