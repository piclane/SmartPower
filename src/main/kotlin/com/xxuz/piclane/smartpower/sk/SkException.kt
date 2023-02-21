package com.xxuz.piclane.smartpower.sk

import com.xxuz.piclane.smartpower.sk.event.EventBase

open class SkException: Exception {
    constructor(message: String, line: String): super("${message}: $line")
    constructor(message: String, event: EventBase): super("${message}: $event")
    constructor(message: String): super(message)
    protected constructor(): super()
}

class SkJoinException: SkException()

class SkTimeoutException: SkException()

class SkEofException: SkException()

class SkIllegalResponseException: SkException {
    constructor(message: String, line: String): super(message, line)
    constructor(message: String): super(message)
}
