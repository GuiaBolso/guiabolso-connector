package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.GcpErrorCode.CLOUD_KMS_FAILURE
import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.utils.decodeBase64
import br.com.guiabolso.connector.common.utils.encodeBase64
import br.com.guiabolso.connector.configuration.GoogleCloud
import br.com.guiabolso.connector.event.exception.CryptographyException
import com.google.api.gax.rpc.ApiException
import com.google.cloud.kms.v1.CryptoKeyPathName
import com.google.cloud.kms.v1.DecryptRequest
import com.google.cloud.kms.v1.EncryptRequest
import com.google.cloud.kms.v1.KeyManagementServiceClient
import com.google.protobuf.ByteString
import org.springframework.stereotype.Service

@GoogleCloud
@Service("configDecryptorService")
class CloudKMSService(
    cryptoKeyPathName: CryptoKeyPathName,
    private val kmsClient: KeyManagementServiceClient
) : CryptographyService {

    private val resourceName = cryptoKeyPathName.toString()

    override fun encrypt(data: ByteArray): EncryptedData {
        val request = EncryptRequest.newBuilder()
            .setName(resourceName)
            .setPlaintext(ByteString.copyFrom(data))
            .build()

        return try {
            kmsClient.encrypt(request).run {
                EncryptedData(ciphertext.toByteArray().encodeBase64())
            }
        } catch (e: ApiException) {
            throw CryptographyException(
                CLOUD_KMS_FAILURE,
                "Could not encrypt data into Cloud KMS",
                e
            )
        }
    }

    override fun decrypt(encryptedData: EncryptedData): ByteArray {
        val request = DecryptRequest.newBuilder()
            .setName(resourceName)
            .setCiphertext(ByteString.copyFrom(encryptedData.value.decodeBase64()))
            .build()

        return try {
            kmsClient.decrypt(request).run { plaintext.toByteArray() }
        } catch (e: ApiException) {
            throw CryptographyException(
                CLOUD_KMS_FAILURE,
                "Could not decrypt data into Cloud KMS",
                e
            )
        }
    }
}
