package com.xxuz.piclane.smartpower.sk

class IpAddr(val value: String) {
    override fun toString() = value
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IpAddr

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
