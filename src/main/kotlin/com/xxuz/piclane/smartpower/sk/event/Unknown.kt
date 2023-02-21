package com.xxuz.piclane.smartpower.sk.event

data class Unknown(
    val components: List<String>
): EventBase {
    companion object {
        fun from(components: List<String>) = Unknown(components)
    }
}
