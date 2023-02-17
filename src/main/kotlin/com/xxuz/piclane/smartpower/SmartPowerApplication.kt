package com.xxuz.piclane.smartpower

import com.fazecast.jSerialComm.SerialPort
import com.xxuz.piclane.smartpower.utils.CrLfPrintWriter
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

@SpringBootApplication
class SmartPowerApplication

fun main(args: Array<String>) {
    val port = SerialPort.getCommPort("/dev/ttyUSB0")

    port.allowElevatedPermissionsRequest()
    port.openPort().also {
        if(!it) {
            println("Error code was " + port.lastErrorCode + " at Line " + port.lastErrorLocation)
            exitProcess(1)
        }
    }
    port.baudRate = 115200
    port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING or SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0)

    val reader = BufferedReader(InputStreamReader(port.inputStream, StandardCharsets.UTF_8))
    val writer = CrLfPrintWriter(OutputStreamWriter(port.outputStream, StandardCharsets.UTF_8))

    writer.println("SKVER")
    println(reader.readLine()) // ECHO BACK
    println(reader.readLine()) // VERSION

    port.closePort()

//    runApplication<SmartPowerApplication>(*args)
}
