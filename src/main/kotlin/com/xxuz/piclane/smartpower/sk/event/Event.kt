package com.xxuz.piclane.smartpower.sk.event

import java.nio.ByteBuffer

data class Event(
    val num: Num,
    val sender: ByteBuffer,
    val param: Int?
): EventBase {
    enum class Num(val value: Int) {
        /** NS を受信した */
        RECEIVED_NS(1),
        /** NA を受信した */
        RECEIVED_NA(2),
        /** Echo Requestを受信した */
        RECEIVED_ECHO_REQUEST(5),
        /** ED スキャンが完了した */
        ED_SCAN_COMPLETED(0x1F),
        /** Beacon を受信した */
        RECEIVED_BEACON(0x20),
        /** UDP 送信処理が完了した */
        UDP_SENDING_COMPLETED(0x21),
        /** アクティブスキャンが完了した */
        ACTIVE_SCAN_COMPLETED(0x22),
        /** PANA による接続過程でエラーが発生した(接続が完了しなかった) */
        PANA_CONNECTION_ERROR(0x24),
        /** PANA による接続が完了した */
        PANA_CONNECTION_COMPLETED(0x25),
        /** 接続相手からセッション終了要求を受信した */
        RECEIVED_SESSION_TERMINATION_REQUEST(0x26),
        /** PANAセッションの終了に成功した */
        PANA_SESSION_TERMINATION_SUCCESS(0x27),
        /** PANAセッションの終了要求に対する応答がなく、タイムアウトした(セッションは終了) */
        PANA_SESSION_TERMINATION_TIMEOUT(0x28),
        /** セッションのライフタイムが経過して期限切れになった */
        SESSION_LIFETIME_EXPIRED(0x29),
        /** ARIB108の送信総和時間の制限が発動した(このイベント以後、あらゆるデータ送信要求が内部で自動的にキャンセルされます) */
        ARIB108_TOTAL_TRANSMISSION_TIME_LIMIT(0x32),
        /** 送信総和時間の制限が解除された */
        TRANSMISSION_TIME_LIMIT_RELEASED(0x33);

        companion object {
            fun valueOf(value: Int) = values().find { it.value == value }
        }
    }
}


