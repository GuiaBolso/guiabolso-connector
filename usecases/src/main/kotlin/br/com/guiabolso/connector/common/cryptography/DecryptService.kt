package br.com.guiabolso.connector.common.cryptography

interface DecryptService {

    fun decrypt(encryptedData: EncryptedData): ByteArray
}
