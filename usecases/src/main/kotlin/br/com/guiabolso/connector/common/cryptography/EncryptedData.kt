package br.com.guiabolso.connector.common.cryptography

import br.com.guiabolso.connector.common.utils.toUTF8String

data class EncryptedData(val value: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    fun stringValue() = value.toUTF8String()
}
