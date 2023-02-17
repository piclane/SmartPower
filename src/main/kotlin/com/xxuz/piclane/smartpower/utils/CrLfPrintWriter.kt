package com.xxuz.piclane.smartpower.utils

import java.io.PrintWriter
import java.io.Writer

class CrLfPrintWriter(out: Writer) : PrintWriter(out) {
    override fun println() {
        synchronized(this.lock) {
            write("\r\n")
        }
    }
}
