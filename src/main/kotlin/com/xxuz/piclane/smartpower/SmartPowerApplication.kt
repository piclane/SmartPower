package com.xxuz.piclane.smartpower

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SmartPowerApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<SmartPowerApplication>(*args)
        }
    }
}
