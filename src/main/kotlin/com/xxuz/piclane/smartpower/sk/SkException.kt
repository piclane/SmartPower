package com.xxuz.piclane.smartpower.sk

open class SkException(message: String, messageRaw: String): Exception("${message}: $messageRaw") {
    constructor(message: String): this(message, "")
}

class SkJoinException(message: String): SkException(message)

class SkTimeoutException(message: String): SkException(message)

class SkEofException(message: String): SkException(message)
