package com.xxuz.piclane.smartpower.sk.event

import com.xxuz.piclane.smartpower.sk.IpAddr
import com.xxuz.piclane.smartpower.sk.parseBoolean
import com.xxuz.piclane.smartpower.sk.parseUInt16
import com.xxuz.piclane.smartpower.sk.parseUInt8Array
import java.nio.ByteBuffer

/**
 * 自端末宛ての UDP(マルチキャスト含む)を受信すると通知されます。
 */
data class RxUdp(
    /** 送信元 IPv6 アドレス */
    val sender: IpAddr,
    /** 送信先 IPv6 アドレス */
    val dest: IpAddr,
    /** 送信元ポート番号 */
    val rPort: Int,
    /** 送信先ポート番号 */
    val lPort: Int,
    /** 送信元の MAC 層アドレス(64bit) */
    val senderLla: ByteBuffer,
    /** 受信した IP パケットを構成する MAC フレームが暗号 化されていた場合 true そうでない場合 false */
    val secured: Boolean,
    /** 受信したデータの長さ */
    val dataLen: Int,
    /** 受信データ */
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
}
