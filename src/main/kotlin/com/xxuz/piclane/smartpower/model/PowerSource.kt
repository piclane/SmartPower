package com.xxuz.piclane.smartpower.model

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class PowerSource(
    /** 定格電流 (A) */
    @param:Value("\${app.powerSource.ratedCurrentA}")
    val ratedCurrentA: Int,

    /** 線式 (2 or 3) */
    @param:Value("\${app.powerSource.wireCount}")
    val wireCount: Int,
) {
    @PostConstruct
    fun validate() {
        if(ratedCurrentA < 10) {
            throw IllegalArgumentException("ratedCurrentA に 10A 未満は指定出来ません: $ratedCurrentA")
        }

        if(ratedCurrentA > 60) {
            throw IllegalArgumentException("ratedCurrentA に 61A 以上は指定出来ません: $ratedCurrentA")
        }

        if(wireCount != 2 && wireCount != 3) {
            throw IllegalArgumentException("wireCount に 2 または 3 以外は指定出来ません: $wireCount")
        }
    }
}
