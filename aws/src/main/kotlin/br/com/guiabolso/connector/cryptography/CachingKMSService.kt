package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.exception.DecryptionException
import br.com.guiabolso.connector.common.utils.decodeBase64
import br.com.guiabolso.connector.common.utils.encodeBase64
import br.com.guiabolso.connector.configuration.Aws
import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager
import com.amazonaws.encryptionsdk.exception.AwsCryptoException
import org.springframework.stereotype.Service

@Aws
@Service
class CachingKMSService(
    private val crypto: AwsCrypto,
    private val cache: CachingCryptoMaterialsManager
) : CryptographyService {

    override fun encrypt(data: ByteArray): EncryptedData {
        return try {
            EncryptedData(value = crypto.encryptData(cache, data).getResult().encodeBase64())
        } catch (e: AwsCryptoException) {
            throw DecryptionException("Could not encrypt KMS-encrypted data due to the following exception", e)
        }
    }

    override fun decrypt(encryptedData: EncryptedData): ByteArray {
        return try {
            crypto.decryptData(cache, encryptedData.value.decodeBase64()).getResult()
        } catch (e: AwsCryptoException) {
            throw DecryptionException("Could not decrypt KMS-encrypted data due to the following exception", e)
        }
    }
}
