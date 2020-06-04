package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.profile.Development
import br.com.guiabolso.connector.common.utils.toUTF8String
import org.springframework.stereotype.Service

@Development
@Service("configDecryptorService")
class StubbedCryptographyService : CryptographyService {

    override fun encrypt(data: ByteArray): EncryptedData {
        return EncryptedData(
            value = "$encryptionPrefix${data.toUTF8String()}".toByteArray()
        )
    }

    override fun decrypt(encryptedData: EncryptedData): ByteArray {
        val data = encryptedData.stringValue()

        require(data.startsWith(encryptionPrefix)) { "Invalid encryption key" }

        return data.removePrefix(encryptionPrefix).toByteArray()
    }

    companion object {
        private const val encryptionPrefix = "encrypted."
    }
}
