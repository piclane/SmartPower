package com.xxuz.piclane.smartpower.sk

import com.xxuz.piclane.smartpower.sk.event.EventBase

/**
 * SkStack が発する一般的な例外
 */
open class SkException: Exception {
    constructor(message: String, line: String): super("${message}: $line")
    constructor(message: String, event: EventBase): super("${message}: $event")
    constructor(message: String): super(message)
    protected constructor(): super()
}

/**
 * SKJOIN の結果 PANA_CONNECTION_ERROR 0x24 イベントが発生した場合
 */
class SkJoinException: SkException()

/**
 * 指定したタイムアウトを超過した場合
 */
class SkTimeoutException: SkException()

/**
 * 予期せぬ切断が発生した場合
 */
class SkEofException: SkException()

/**
 * 不正なレスポンスを受信した場合
 */
class SkIllegalResponseException: SkException {
    constructor(message: String, line: String): super(message, line)
    constructor(message: String): super(message)
}
