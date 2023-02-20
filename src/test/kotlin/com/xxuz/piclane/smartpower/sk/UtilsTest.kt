package com.xxuz.piclane.smartpower.sk

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class UtilsTest {

    @Test
    fun test_parseUInt16() {
        assertThat(parseUInt16("0E1A")).isEqualTo(3610)
    }

    @Test
    fun test_toUInt16() {
        assertThat(toUInt16(3610)).isEqualTo("0E1A")
    }

    @Test
    fun test_toUInt32() {
        assertThat(toUInt32(0xffffffff)).isEqualTo("FFFFFFFF")
        assertThat(toUInt32(0x1)).isEqualTo("00000001")
    }

    @Test
    fun test_toUInt8Array() {
        val b = parseUInt8Array("88")
        val b1 = b.get()
        val i1 = java.lang.Byte.toUnsignedInt(b1)
        println(i1)
    }
}
