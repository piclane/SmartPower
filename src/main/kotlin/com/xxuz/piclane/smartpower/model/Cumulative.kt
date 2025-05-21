package com.xxuz.piclane.smartpower.model

/**
 * 積算計測値
 */
data class Cumulative(
    /** 正方向積算電力量 (kWh) */
    val forwardEnergy: Double,
)
