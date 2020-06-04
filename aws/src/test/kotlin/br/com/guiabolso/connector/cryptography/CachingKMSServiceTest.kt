package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.exception.DecryptionException
import br.com.guiabolso.connector.common.utils.decodeBase64
import br.com.guiabolso.connector.common.utils.encodeBase64
import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager
import com.amazonaws.encryptionsdk.exception.AwsCryptoException
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CachingKMSServiceTest {

    private lateinit var crypto: AwsCrypto
    private lateinit var cache: CachingCryptoMaterialsManager
    private lateinit var service: CachingKMSService

    @BeforeEach
    fun setUp() {
        crypto = mock()
        cache = mock()

        service = CachingKMSService(crypto, cache)
    }

    @Test
    fun `should encrypt`() {
        val data = "some-data".toByteArray()
        val encryptedData = "some-encrypted-data".toByteArray()

        val cryptoResult: CryptoResult<ByteArray, *> = mock()
        whenever(crypto.encryptData(cache, data)).thenReturn(cryptoResult)
        whenever(cryptoResult.getResult()).thenReturn(encryptedData)

        val actual = service.encrypt(data)

        assertThat(actual.value.contentToString()).isEqualTo(encryptedData.encodeBase64().contentToString())

        verify(crypto).encryptData(cache, data)
    }

    @Test
    fun `should decrypt`() {
        val encryptedData = EncryptedData(value = "some-data".toByteArray().encodeBase64())
        val data = "some-data".toByteArray()

        val cryptoResult: CryptoResult<ByteArray, *> = mock()
        whenever(crypto.decryptData(cache, encryptedData.value.decodeBase64())).thenReturn(cryptoResult)
        whenever(cryptoResult.getResult()).thenReturn(data)

        val actual = service.decrypt(encryptedData)

        assertThat(actual.contentToString()).isEqualTo(data.contentToString())

        verify(crypto).decryptData(cache, encryptedData.value.decodeBase64())
    }

    @Test
    fun `should throw exception when try to decrypt and a AWSKMSException is thrown`() {
        val encryptedData = EncryptedData(value = "some-data".toByteArray().encodeBase64())

        whenever(crypto.decryptData(cache, encryptedData.value.decodeBase64()))
            .thenThrow(AwsCryptoException::class.java)

        assertThatExceptionOfType(DecryptionException::class.java)
            .isThrownBy { service.decrypt(encryptedData) }

        verify(crypto).decryptData(cache, encryptedData.value.decodeBase64())
    }
}
