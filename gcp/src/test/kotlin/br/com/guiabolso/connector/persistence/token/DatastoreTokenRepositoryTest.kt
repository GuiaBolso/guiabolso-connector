package br.com.guiabolso.connector.persistence.token

import br.com.guiabolso.connector.GcpErrorCode.DATASTORE_FAILURE
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.PersistenceException
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS

@TestInstance(PER_CLASS)
class DatastoreTokenRepositoryTest {

    private lateinit var configService: ConfigService
    private lateinit var datastoreService: Datastore
    private lateinit var repository: DatastoreTokenRepository

    private val helper = LocalDatastoreHelper.create().also { it.start() }

    @AfterAll
    fun tearDown() {
        helper.stop()
    }

    @BeforeEach
    fun setUp() {
        configService = mock()
        whenever(configService.getRequiredString(DATASTORE_KIND_TOKEN_KEY)).thenReturn(DATASTORE_KIND_TOKEN_VALUE)

        datastoreService = helper.options.service

        repository = DatastoreTokenRepository(configService, datastoreService)
    }

    @Test
    fun `should insert token`() {
        val userId = nextObject<String>()
        val accessToken = EncryptedData(nextObject<String>().toByteArray())
        val refreshToken = EncryptedData(nextObject<String>().toByteArray())

        repository.putToken(userId, accessToken, refreshToken)

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(accessToken)
    }

    @Test
    fun `should not find access token`() {
        val userId = nextObject<String>()

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isNull()
    }

    @Test
    fun `should find access token`() {
        val userId = nextObject<String>()
        val accessToken = EncryptedData(nextObject<String>().toByteArray())
        val refreshToken = EncryptedData(nextObject<String>().toByteArray())

        repository.putToken(userId, accessToken, refreshToken)

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(accessToken)
    }

    @Test
    fun `should not find refresh token`() {
        val userId = nextObject<String>()

        val actual = repository.findRefreshTokenBy(userId)

        assertThat(actual).isNull()
    }

    @Test
    fun `should find refresh token`() {
        val userId = nextObject<String>()
        val accessToken = EncryptedData(nextObject<String>().toByteArray())
        val refreshToken = EncryptedData(nextObject<String>().toByteArray())

        repository.putToken(userId, accessToken, refreshToken)

        val actual = repository.findRefreshTokenBy(userId)

        assertThat(actual).isEqualTo(refreshToken)
    }

    @Test
    fun `should throw exception when try to update an nonexistent token`() {
        val userId = nextObject<String>()
        val newAccessToken = EncryptedData(nextObject<String>().toByteArray())

        assertThatExceptionOfType(PersistenceException::class.java).isThrownBy {
            repository.updateAccessToken(userId, newAccessToken)
        }.withMessage(DATASTORE_FAILURE)
    }

    @Test
    fun `should update access token`() {
        val userId = nextObject<String>()
        val accessToken = EncryptedData(nextObject<String>().toByteArray())
        val refreshToken = EncryptedData(nextObject<String>().toByteArray())

        repository.putToken(userId, accessToken, refreshToken)

        val newAccessToken = EncryptedData(nextObject<String>().toByteArray())

        repository.updateAccessToken(userId, newAccessToken)

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(newAccessToken)
    }

    companion object {
        private const val DATASTORE_KIND_TOKEN_KEY = "datastore.kind.token"
        private const val DATASTORE_KIND_TOKEN_VALUE = "token"
    }
}
