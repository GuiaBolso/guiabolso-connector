package br.com.guiabolso.connector.common.cryptography

interface CryptographyService : DecryptService {

    fun encrypt(data: ByteArray): EncryptedData
}
