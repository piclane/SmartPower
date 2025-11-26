package com.xxuz.piclane.smartpower.sk

import java.nio.ByteBuffer

@Suppress("UNUSED_PARAMETER")
data class EchoNetLiteFrame(
    /** ECHONET Lite電文ヘッダー1 uint8 (02-3.2.1.1) */
    val ehd1: Int,
    /** ECHONET Lite電文ヘッダー2 uint8 (02-3.2.1.2) */
    val ehd2: Int,
    /** トランザクションID uint16 (02-3.2.2) */
    val tid: Int,
    /** ECHONET Liteデータ (02-3.2.3) */
    val edata: EDATA,
) {
    /** サイズ (bytes) */
    internal val size get() = 4 + edata.size

    /**
     * ECHONET Lite データ (02-3.2.3)
     *
     * ECHONET Lite 通信ミドルウェアにてやり取りされる電文のデータ領域
     */
    data class EDATA(
        /** 送信元 ECHONET Lite オブジェクト指定 */
        val sEoj: EOJ,
        /** 送信先 ECHONET Lite オブジェクト指定 */
        val dEoj: EOJ,
        /** ECHONET Liteサービス uint8 (02-3.2.5) */
        val esv: Int,
        /** 処理 (02-3.2.6) */
        val op: List<OP>,
    ) {
        /** 処理プロパティ数 uint8 (02-3.2.6) */
        val opc get() = op.size

        companion object {
            /**
             * ByteBuffer の現在位置からの内容でインスタンスを生成します
             */
            internal fun from(buf: ByteBuffer): EDATA {
                val sEoj = EOJ.from(buf)
                val dEoj = EOJ.from(buf)
                val esv = parseUInt8(buf.get())
                val opc = parseUInt8(buf.get())
                val op = sequence { repeat(opc) { yield(OP.from(buf)) } }.toList()
                return EDATA(sEoj, dEoj, esv, op)
            }
        }

        /** サイズ (bytes) */
        internal val size get() = sEoj.size + dEoj.size + 2 + op.sumOf { it.size }
        /**
         * ByteBuffer に書き込みます
         */
        internal fun putInto(buf: ByteBuffer) {
            sEoj.putInto(buf)
            dEoj.putInto(buf)
            buf.put(esv.toByte())
            buf.put(opc.toByte())
            op.forEach { it.putInto(buf) }
        }

        override fun toString(): String {
            return buildString {
                append("EDATA(")
                append("sEoj=").append(sEoj).append(", ")
                append("dEoj=").append(dEoj).append(", ")
                append("esv=").append(esv.hex8()).append(", ")
                append("opc=").append(opc.hex8()).append(", ")
                append("op=").append(op.joinToString(prefix = "[", postfix = "]"))
                append(")")
            }
        }
    }

    /**
     * ECHONET オブジェクト (02-3.2.4)
     */
    data class EOJ(
        /** クラスグループコード (0x00~0xFF) */
        val classGroupCode: Int,
        /** クラスコード (0x00~0xFF) */
        val classCode: Int,
        /** インスタンスコード (0x00~0x7F) */
        val instanceCode: Int,
    ) {
        companion object {
            /**
             * ByteBuffer の現在位置からの内容でインスタンスを生成します
             */
            internal fun from(buf: ByteBuffer): EOJ {
                val classGroupCode = parseUInt8(buf.get())
                val classCode = parseUInt8(buf.get())
                val instanceCode = parseUInt8(buf.get())
                return EOJ(classGroupCode, classCode, instanceCode)
            }
        }

        /** サイズ (bytes) */
        internal val size get() = 3
        /**
         * ByteBuffer に書き込みます
         */
        internal fun putInto(buf: ByteBuffer) {
            buf.put(classGroupCode.toByte())
            buf.put(classCode.toByte())
            buf.put(instanceCode.toByte())
        }

        override fun toString(): String {
            return buildString {
                append("EOJ(")
                append("classGroupCode=").append(classGroupCode.hex8()).append(", ")
                append("classCode=").append(classCode.hex8()).append(", ")
                append("instanceCode=").append(instanceCode.hex8())
                append(")")
            }
        }
    }

    data class OP(
        /** ECHONET Liteプロパティ uint8 (02-3.2.7) */
        val epc: Int,
        /** プロパティ値データ 可変長 (02-3.2.9) */
        val edt: List<Byte> = emptyList(),
    ) {
        /** EDT のバイト数 unit8 (02-3.2.8) */
        val pdc get() = edt.size

        /** EDT を ByteBuffer に変換します */
        fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(edt.toByteArray())

        companion object {
            /**
             * ByteBuffer の現在位置からの内容でインスタンスを生成します
             */
            internal fun from(buf: ByteBuffer): OP {
                val epc = parseUInt8(buf.get())
                val pdc = parseUInt8(buf.get())
                val edt = sequence { repeat(pdc) { yield(buf.get()) } }.toList()
                return OP(epc, edt)
            }
        }

        /** サイズ (bytes) */
        internal val size get() = 2 + edt.size
        /**
         * ByteBuffer に書き込みます
         */
        internal fun putInto(buf: ByteBuffer) {
            buf.put(epc.toByte())
            buf.put(pdc.toByte())
            buf.put(edt.toByteArray())
        }

        override fun toString(): String {
            val edtHex = edt.joinToString(prefix = "[", postfix = "]") { it.hex8() }
            return buildString {
                append("OP(")
                append("epc=").append(epc.hex8()).append(", ")
                append("pdc=").append(pdc.hex8()).append(", ")
                append("edt=").append(edtHex)
                append(")")
            }
        }
    }

    companion object {
        /**
         * ByteBuffer の現在位置からの内容でインスタンスを生成します
         */
        @JvmStatic
        fun from(buf: ByteBuffer): EchoNetLiteFrame {
            val ehd1 = parseUInt8(buf.get())
            val ehd2 = parseUInt8(buf.get())
            val tid = parseUInt16(buf.short)
            val edata = EDATA.from(buf)
            return EchoNetLiteFrame(ehd1, ehd2, tid, edata)
        }

        @JvmStatic
        fun fromHex(hex: String): EchoNetLiteFrame = from(parseUInt8Array(hex))
    }

    /**
     * バイト配列を生成します
     */
    fun toByteArray(): ByteArray {
        val buf = ByteBuffer.allocate(size)
        buf.put(ehd1.toByte())
        buf.put(ehd2.toByte())
        buf.putShort(tid.toShort())
        edata.putInto(buf)
        buf.rewind()
        return buf.array()
    }

    override fun toString(): String {
        return buildString {
            append("EchoNetLiteFrame(")
            append("ehd1=").append(ehd1.hex8()).append(", ")
            append("ehd2=").append(ehd2.hex8()).append(", ")
            append("tid=").append(tid.hex16()).append(", ")
            append("edata=").append(edata)
            append(")")
        }
    }
}

// --- private helpers for hex formatting ---
private fun Int.hex8(): String = "0x" + this.toString(16).uppercase().padStart(2, '0')
private fun Int.hex16(): String = "0x" + this.toString(16).uppercase().padStart(4, '0')
private fun Byte.hex8(): String = "0x" + ((this.toInt()) and 0xFF).toString(16).uppercase().padStart(2, '0')
