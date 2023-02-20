package com.xxuz.piclane.smartpower.sk.command

enum class SendToSecurity(val value: Int) {
    /**
     * 必ず平文で送信します。
     */
    PLAIN(0),

    /**
     * 送信先がセキュリティ有効で登録されている場合は暗号化して送信します。
     * 登録されていない場合、または、暗号化無しで登録されている場合、データは送信されません。
     */
    ENCRYPT(1),

    /**
     * 送信先がセキュリティ有効で登録されている場合は暗号化して送信します。
     * 登録されていない場合、または、暗号化無しで登録されている場合、データは平文で送信されます。
     */
    ENCRYPT_OR_PLAIN(2);
}
