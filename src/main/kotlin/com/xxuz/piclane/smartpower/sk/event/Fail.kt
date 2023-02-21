package com.xxuz.piclane.smartpower.sk.event

data class Fail(
    /** エラーコード */
    val code: String
): EventBase {
    companion object {
        fun from(components: List<String>): Fail = Fail(
            code = components[1]
        )
    }

    override fun toString(): String = "FAIL $code"
}
