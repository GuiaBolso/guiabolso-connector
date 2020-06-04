package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.exception.DecryptionException
import br.com.guiabolso.connector.common.utils.decodeBase64
import br.com.guiabolso.connector.common.utils.encodeBase64
import br.com.guiabolso.connector.common.utils.toByteBuffer
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.model.AWSKMSException
import com.amazonaws.services.kms.model.DecryptRequest
import com.amazonaws.services.kms.model.DecryptResult
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KMSConfigDecryptorServiceTest {

    private lateinit var kmsClient: AWSKMS
    private lateinit var service: KMSConfigDecryptorService

    @BeforeEach
    fun setUp() {
        kmsClient = mock()

        service = KMSConfigDecryptorService(kmsClient)
    }

    @Test
    fun `should decrypt`() {
        val encryptedData = EncryptedData(value = "some-encrypted-data".toByteArray().encodeBase64())
        val data = "some-data".toByteArray()

        val decryptRequest = DecryptRequest().withCiphertextBlob(encryptedData.value.decodeBase64().toByteBuffer())

        val decryptResult: DecryptResult = mock()

        whenever(decryptResult.plaintext).thenReturn(data.toByteBuffer())
        whenever(kmsClient.decrypt(decryptRequest)).thenReturn(decryptResult)

        val actual = service.decrypt(encryptedData)

        assertThat(actual.contentToString()).isEqualTo(data.contentToString())

        verify(kmsClient).decrypt(decryptRequest)
    }

    @Test
    fun `should throw exception when try to decrypt and a AWSKMSException is thrown`() {
        val encryptedData = EncryptedData(value = "some-data".toByteArray().encodeBase64())
        val decryptRequest = DecryptRequest().withCiphertextBlob(encryptedData.value.decodeBase64().toByteBuffer())

        whenever(kmsClient.decrypt(decryptRequest)).thenThrow(AWSKMSException("some-message"))

        assertThatExceptionOfType(DecryptionException::class.java)
            .isThrownBy { service.decrypt(encryptedData) }

        verify(kmsClient).decrypt(decryptRequest)
    }
}
