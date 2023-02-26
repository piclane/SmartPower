package com.xxuz.piclane.smartpower.model

/**
 * 電流
 */
data class Current(
    /** R相 */
    val rPhase: Double,
    /** T相 */
    val tPhase: Double,
) {
    companion object {
        /** ゼロ */
        val ZERO = Current(0.0, 0.0)
    }

    /** 合計 */
    val sum get() = rPhase + tPhase
}
