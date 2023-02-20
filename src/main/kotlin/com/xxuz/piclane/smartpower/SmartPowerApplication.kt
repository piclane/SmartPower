package com.xxuz.piclane.smartpower

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SmartPowerApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<SmartPowerApplication>(*args)/*.use { ctx ->
                val app = ctx.getBean(SmartPowerApplication::class.java)

                app.start()
            }*/
        }
    }

//    fun start() {
//
//    }
}
