package br.com.guiabolso.connector.cryptography

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StubbedCryptographyServiceTest {

    private lateinit var service: StubbedCryptographyService

    @BeforeEach
    fun setUp() {
        service = StubbedCryptographyService()
    }

    @Test
    fun `should encrypt`() {
        val data = "some-data".toByteArray()
        val encryptedData = "encrypted.some-data".toByteArray()

        val actual = service.encrypt(data)

        assertThat(actual.value.contentToString()).isEqualTo(encryptedData.contentToString())
    }

    @Test
    fun `should decrypt`() {
        val encryptedData =
            EncryptedData(value = "encrypted.some-data".toByteArray())
        val data = "some-data".toByteArray()

        val actual = service.decrypt(encryptedData)

        assertThat(actual.contentToString()).isEqualTo(data.contentToString())
    }

    @Test
    fun `should throw exception when try to decrypt an invalid ciphertext`() {
        val encryptedData =
            EncryptedData(value = "some-data-encrypted".toByteArray())

        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { service.decrypt(encryptedData) }
    }
}
