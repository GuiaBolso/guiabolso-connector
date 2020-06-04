package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.common.cryptography.DecryptService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.exception.DecryptionException
import br.com.guiabolso.connector.common.utils.decodeBase64
import br.com.guiabolso.connector.common.utils.toByteBuffer
import br.com.guiabolso.connector.configuration.Aws
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.AWSKMSException
import com.amazonaws.services.kms.model.DecryptRequest
import org.springframework.stereotype.Service

@Aws
@Service("configDecryptorService")
class KMSConfigDecryptorService(
    private val kmsClient: AWSKMS
) : DecryptService {

    override fun decrypt(encryptedData: EncryptedData): ByteArray {
        val request = DecryptRequest().withCiphertextBlob(encryptedData.value.decodeBase64().toByteBuffer())

        return try {
            kmsClient.decrypt(request).plaintext!!.array()
        } catch (e: AWSKMSException) {
            throw DecryptionException("Could not decrypt KMS-encrypted data due to the following exception", e)
        }
    }
}
