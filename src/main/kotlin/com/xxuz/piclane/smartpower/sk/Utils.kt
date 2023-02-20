package com.xxuz.piclane.smartpower.sk

import org.apache.commons.codec.binary.Hex
import java.nio.ByteBuffer

fun parseUInt8(string: String) = Integer.parseUnsignedInt(string, 16)

fun parseUInt8(byte: Byte) = java.lang.Byte.toUnsignedInt(byte)

fun parseUInt16(string: String) = Integer.parseUnsignedInt(string, 16)

fun parseUInt16(short: Short) = java.lang.Short.toUnsignedInt(short)

fun parseUInt8Array(string: String): ByteBuffer = ByteBuffer.wrap(Hex.decodeHex(string))

fun parseBoolean(string: String): Boolean = parseUInt8(string) != 0

fun toUInt8(value: Int): String = value.toString(16).uppercase()

fun toUInt16(value: Int): String = value.toString(16).uppercase().padStart(4, '0')

fun toUInt32(value: Long): String = value.toString(16).uppercase().padStart(8, '0')

fun toUInt8Array(bytes: ByteBuffer): String = Hex.encodeHexString(bytes, false)
