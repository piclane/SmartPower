package com.xxuz.piclane.smartpower.power

/**
 * 瞬時計測値
 */
data class Instantaneous(
    /** 電力計測値 (W) */
    val power: Int,
    /** 電流計測値 */
    val current: Current
) {
    companion object {
        val ZERO = Instantaneous(
            power = 0,
            current = Current.ZERO
        )
    }
}
