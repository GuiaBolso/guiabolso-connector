package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.utils.decodeBase64
import br.com.guiabolso.connector.common.utils.encodeBase64
import br.com.guiabolso.connector.event.exception.CryptographyException
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import com.google.api.gax.rpc.ApiException
import com.google.cloud.kms.v1.CryptoKeyPathName
import com.google.cloud.kms.v1.DecryptRequest
import com.google.cloud.kms.v1.DecryptResponse
import com.google.cloud.kms.v1.EncryptRequest
import com.google.cloud.kms.v1.EncryptResponse
import com.google.cloud.kms.v1.KeyManagementServiceClient
import com.google.protobuf.ByteString
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CloudKMSServiceTest {

    private lateinit var cryptoKeyPathName: CryptoKeyPathName
    private lateinit var kmsClient: KeyManagementServiceClient
    private lateinit var service: CloudKMSService

    @BeforeEach
    fun setUp() {
        cryptoKeyPathName = mock()
        whenever(cryptoKeyPathName.toString()).thenReturn(RESOURCE_NAME)

        kmsClient = mock()

        service = CloudKMSService(cryptoKeyPathName, kmsClient)
    }

    @Test
    fun `should throw exception when try to encrypt and Cloud KMS fails`() {
        val decryptedData = nextObject<ByteArray>()

        val request = EncryptRequest.newBuilder()
            .setName(RESOURCE_NAME)
            .setPlaintext(ByteString.copyFrom(decryptedData))
            .build()

        whenever(kmsClient.encrypt(request)).thenThrow(ApiException::class.java)

        assertThatExceptionOfType(CryptographyException::class.java)
            .isThrownBy { service.encrypt(decryptedData) }

        verify(kmsClient).encrypt(request)
    }

    @Test
    fun `should encrypt data`() {
        val decryptedData = nextObject<ByteArray>()

        val request = EncryptRequest.newBuilder()
            .setName(RESOURCE_NAME)
            .setPlaintext(ByteString.copyFrom(decryptedData))
            .build()

        val response = EncryptResponse.newBuilder()
            .setCiphertext(ByteString.copyFrom(decryptedData.encodeBase64()))
            .build()

        whenever(kmsClient.encrypt(request)).thenReturn(response)

        service.encrypt(decryptedData)

        verify(kmsClient).encrypt(request)
    }

    @Test
    fun `should throw exception when try to decrypt and Cloud KMS fails`() {
        val encryptedData = EncryptedData(nextObject<ByteArray>().encodeBase64())

        val request = DecryptRequest.newBuilder()
            .setName(RESOURCE_NAME)
            .setCiphertext(ByteString.copyFrom(encryptedData.value.decodeBase64()))
            .build()

        whenever(kmsClient.decrypt(request)).thenThrow(ApiException::class.java)

        assertThatExceptionOfType(CryptographyException::class.java)
            .isThrownBy { service.decrypt(encryptedData) }

        verify(kmsClient).decrypt(request)
    }

    @Test
    fun `should return decrypted data`() {
        val decryptedData = nextObject<ByteArray>()

        val encryptedData = EncryptedData(decryptedData.encodeBase64())

        val request = DecryptRequest.newBuilder()
            .setName(RESOURCE_NAME)
            .setCiphertext(ByteString.copyFrom(encryptedData.value.decodeBase64()))
            .build()

        val response = DecryptResponse.newBuilder()
            .setPlaintext(ByteString.copyFrom(decryptedData))
            .build()

        whenever(kmsClient.decrypt(request)).thenReturn(response)

        val actual = service.decrypt(encryptedData)

        assertThat(actual).isEqualTo(decryptedData)

        verify(kmsClient).decrypt(request)
    }

    companion object {
        private const val RESOURCE_NAME = "test"
    }
}
