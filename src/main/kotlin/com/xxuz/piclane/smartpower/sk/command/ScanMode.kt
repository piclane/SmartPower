package com.xxuz.piclane.smartpower.sk.command

enum class ScanMode(val value: Int) {
    /** ED スキャン */
    ED_SCAN(0),
    /** アクティブスキャン (IE あり) */
    ACTIVE_SCAN_WITH_IE(2),
    /** アクティブスキャン (IE なし) */
    ACTIVE_SCAN_WITHOUT_IE(3);
}
