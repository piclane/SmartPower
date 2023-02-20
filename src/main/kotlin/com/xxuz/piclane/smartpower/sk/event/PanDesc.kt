package com.xxuz.piclane.smartpower.sk.event

import com.xxuz.piclane.smartpower.sk.toUInt16
import com.xxuz.piclane.smartpower.sk.toUInt8
import java.nio.ByteBuffer

/**
 * アクティブスキャンを実行して発見した PAN
 */
data class PanDesc(
    /** 発見した PAN の周波数(論理チャンネル番号) */
    val channel: Int,
    /** 発見した PAN のチャンネルページ */
    val channelPane: Int,
    /** 発見した PAN の PAN ID */
    val panId: Int,
    /** アクティブスキャン応答元のアドレス */
    val addr: ByteBuffer,
    /** 受信したビーコンの受信 RSSI (LQI – 107dBm) */
    val lqi: Int,
    /** (IEが含まれる場合)相手から受信したPairing ID */
    val pairId: String?,
): EventBase {
    val channelHex get() = toUInt8(channel)

    val panIdHex get() = toUInt16(panId)
}
