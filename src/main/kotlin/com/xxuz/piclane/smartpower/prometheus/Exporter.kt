package com.xxuz.piclane.smartpower.prometheus

import com.xxuz.piclane.smartpower.model.Cumulative
import com.xxuz.piclane.smartpower.power.PowerObserver
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Exporter(
    private val powerObserver: PowerObserver,
    private val meterRegistry: MeterRegistry,
) {
    companion object {
        /** ロガー */
        private val logger = LoggerFactory.getLogger(Exporter::class.java)
    }

    private val cumulativeForwardEnergyCounter = Counter
        .builder("cumulative_forward_energy")
        .description("正方向積算電力量 (kWh)")
        .register(meterRegistry)

    private val handlePropertyChange = { evt: java.beans.PropertyChangeEvent ->
        if (evt.propertyName == PowerObserver.PROPERTY_CUMULATIVE) {
            val oldVal = evt.oldValue as Cumulative?
            val newVal = evt.newValue as Cumulative?
            if (oldVal != null && newVal != null) {
                cumulativeForwardEnergyCounter.increment(newVal.forwardEnergy - oldVal.forwardEnergy)
            }
        }
    }

    @PostConstruct
    fun init() {
        Gauge.builder("instantaneous_power", powerObserver) { it.getInstantaneous().power.toDouble() }
           .description("電力計測値 (W)")
           .register(meterRegistry)

        Gauge.builder("instantaneous_current_phase_r", powerObserver) { it.getInstantaneous().current.rPhase }
            .description("電流計測値 R相 (A)")
            .register(meterRegistry)

        Gauge.builder("instantaneous_current_phase_t", powerObserver) { it.getInstantaneous().current.tPhase }
            .description("電流計測値 T相 (A)")
            .register(meterRegistry)

        powerObserver.addPropertyChangeListener(handlePropertyChange)

        logger.info("Exporter が初期化されました")
    }

    @PreDestroy
    fun destroy() {
        powerObserver.removePropertyChangeListener(handlePropertyChange)

        logger.info("Exporter が破棄されました")
    }
}
