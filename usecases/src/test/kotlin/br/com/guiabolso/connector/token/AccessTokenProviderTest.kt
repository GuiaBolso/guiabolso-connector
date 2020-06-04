package br.com.guiabolso.connector.token

import br.com.guiabolso.connector.common.cache.CacheService
import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.connector.helper.createJWT
import br.com.guiabolso.connector.token.exception.UnauthorizedUserException
import br.com.guiabolso.connector.token.model.Token
import br.com.guiabolso.connector.token.repository.TokenRepository
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.context.EventContext
import br.com.guiabolso.events.context.EventContextHolder
import br.com.guiabolso.events.model.EventErrorType.Generic
import br.com.guiabolso.events.model.EventErrorType.Unauthorized
import br.com.guiabolso.events.model.RequestEvent
import com.google.gson.JsonObject
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import java.time.Duration
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccessTokenProviderTest {

    private lateinit var repository: TokenRepository
    private lateinit var dispatcher: EventDispatcher
    private lateinit var cacheService: CacheService
    private lateinit var cryptographyService: CryptographyService
    private lateinit var dateTimeProvider: ZonedDateTimeProvider

    private lateinit var provider: AccessTokenProvider

    @BeforeEach
    fun setUp() {
        repository = mock()
        dispatcher = mock()
        cacheService = mock()
        cryptographyService = mock()
        dateTimeProvider = mock()

        provider = AccessTokenProvider(repository, dispatcher, cacheService, cryptographyService, dateTimeProvider)
    }

    @Test
    fun `should throw exception when user has no access token`() {
        val userId = nextObject<String>()
        val key = Token.getKey(userId)

        whenever(dateTimeProvider.now()).thenReturn(ZonedDateTime.now())
        whenever(cacheService.getData(key, DURATION, ONLY_IN_MEMORY)).thenReturn(null)
        whenever(repository.findAccessTokenBy(userId)).thenReturn(null)

        assertThatExceptionOfType(UnauthorizedUserException::class.java)
            .isThrownBy { provider.getAccessTokenBy(userId) }

        verify(cacheService).getData(key, DURATION, ONLY_IN_MEMORY)
        verify(repository).findAccessTokenBy(userId)
        verifyZeroInteractions(cryptographyService)
    }

    @Test
    fun `should return access token from cache`() {
        whenever(dateTimeProvider.now()).thenReturn(
            ZonedDateTime.of(2019, 4, 20, 16, 40, 0, 0, UTC)
        )

        val userId = nextObject<String>()
        val key = Token.getKey(userId)
        val decryptedAccessToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw"
        val accessToken = EncryptedData("encrypted-$decryptedAccessToken".toByteArray())

        whenever(cacheService.getData(key, DURATION, ONLY_IN_MEMORY)).thenReturn(accessToken)
        whenever(cryptographyService.decrypt(accessToken)).thenReturn(decryptedAccessToken.toByteArray())

        val actual = provider.getAccessTokenBy(userId)

        assertThat(actual).isEqualTo(decryptedAccessToken)

        verify(cacheService).getData(key, DURATION, ONLY_IN_MEMORY)
        verify(cryptographyService).decrypt(accessToken)
        verifyNoMoreInteractions(cacheService)
        verifyZeroInteractions(repository)
    }

    @Test
    fun `should return access token from repository`() {
        whenever(dateTimeProvider.now()).thenReturn(
            ZonedDateTime.of(2019, 4, 20, 16, 40, 0, 0, UTC)
        )

        val userId = nextObject<String>()
        val key = Token.getKey(userId)
        val decryptedAccessToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw"
        val accessToken = EncryptedData("encrypted-$decryptedAccessToken".toByteArray())

        whenever(cacheService.getData(key, DURATION, ONLY_IN_MEMORY)).thenReturn(null)
        whenever(repository.findAccessTokenBy(userId)).thenReturn(accessToken)
        whenever(cryptographyService.decrypt(accessToken)).thenReturn(decryptedAccessToken.toByteArray())

        val actual = provider.getAccessTokenBy(userId)

        assertThat(actual).isEqualTo(decryptedAccessToken)

        verify(cacheService).getData(key, DURATION, ONLY_IN_MEMORY)
        verify(repository).findAccessTokenBy(userId)
        verify(cacheService).putData(key, accessToken, DURATION, ONLY_IN_MEMORY)
        verify(cryptographyService).decrypt(accessToken)
    }

    @Test
    fun `should throw exception when user has no refresh token`() {
        whenever(dateTimeProvider.now()).thenReturn(
            ZonedDateTime.of(2019, 4, 20, 16, 40, 0, 0, UTC)
        )
        val userId = nextObject<String>()

        val decryptedAccessToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjg0NDM3MTgzMX0.Pnrxw8d6w1wLuK7s_TTKG26vdmW7phpXL7CQewYkKmw"
        val accessToken = EncryptedData("encrypted-$decryptedAccessToken".toByteArray())

        whenever(cacheService.getData(any(), any(), any())).thenReturn(accessToken)
        whenever(cryptographyService.decrypt(accessToken)).thenReturn(decryptedAccessToken.toByteArray())

        whenever(repository.findAccessTokenBy(userId)).thenReturn(accessToken)
        whenever(repository.findRefreshTokenBy(userId)).thenReturn(null)

        assertThatExceptionOfType(UnauthorizedUserException::class.java)
            .isThrownBy { provider.getAccessTokenBy(userId) }

        verify(repository).findRefreshTokenBy(userId)
        verify(cacheService).getData(Token.getKey(userId), Duration.ofMinutes(5), true)
        verifyNoMoreInteractions(repository, cacheService)
        verifyZeroInteractions(dispatcher)
    }

    @Test
    fun `should return refreshed access token when it has only 30 seconds remaining to expire`() {
        val now = ZonedDateTime.now()
        whenever(dateTimeProvider.now()).thenReturn(now)

        EventContextHolder.setContext(
            context = EventContext(
                id = nextObject(),
                flowId = nextObject()
            )
        )

        val userId = nextObject<String>()
        val key = Token.getKey(userId)
        val decryptedRefreshToken = createJWT(issuedAt = now, expireAt = now.plusHours(2))
        val refreshToken = EncryptedData("encrypted-$decryptedRefreshToken".toByteArray())
        val decryptedAccessToken = createJWT(issuedAt = now, expireAt = now.plusSeconds(30))
        val accessToken = EncryptedData("encrypted-$decryptedAccessToken".toByteArray())

        val responseEvent = EventBuilder.responseFor(buildEvent()) {}.copy(
            payload = JsonObject().apply {
                addProperty("accessToken", decryptedAccessToken)
            }
        )

        whenever(cacheService.getData(any(), any(), any())).thenReturn(accessToken)
        whenever(cryptographyService.decrypt(accessToken)).thenReturn(decryptedAccessToken.toByteArray())
        whenever(repository.findRefreshTokenBy(userId)).thenReturn(refreshToken)
        whenever(cryptographyService.decrypt(refreshToken)).thenReturn(decryptedRefreshToken.toByteArray())
        whenever(dispatcher.sendEvent(any())).thenReturn(responseEvent)
        whenever(cryptographyService.encrypt(decryptedAccessToken.toByteArray())).thenReturn(accessToken)

        val actual = provider.getAccessTokenBy(userId)

        assertThat(actual).isEqualTo(decryptedAccessToken)

        verify(repository).findRefreshTokenBy(userId)
        verify(cryptographyService).decrypt(refreshToken)

        argumentCaptor<RequestEvent>().run {
            verify(dispatcher).sendEvent(capture())
            assertThat(firstValue.payload.asJsonObject.get("refreshToken").asString).isEqualTo(decryptedRefreshToken)
        }

        verify(cryptographyService).encrypt(decryptedAccessToken.toByteArray())
        verify(repository).updateAccessToken(userId, accessToken)
        verify(cacheService).putData(key, accessToken, DURATION, ONLY_IN_MEMORY)
    }

    @Test
    fun `should throw unauthorized user exception for unauthorized event error received from guiabolso`() {
        whenever(dateTimeProvider.now()).thenReturn(
            ZonedDateTime.of(2019, 4, 20, 16, 40, 0, 0, UTC)
        )

        EventContextHolder.setContext(
            context = EventContext(
                id = nextObject<String>(),
                flowId = nextObject<String>()
            )
        )

        val userId = nextObject<String>()
        val decryptedRefreshToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw"
        val refreshToken = EncryptedData("encrypted-$decryptedRefreshToken".toByteArray())
        val decryptedAccessToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjg0NDM3MTgzMX0.Pnrxw8d6w1wLuK7s_TTKG26vdmW7phpXL7CQewYkKmw"
        val accessToken = EncryptedData("encrypted-$decryptedAccessToken".toByteArray())

        whenever(cacheService.getData(any(), any(), any())).thenReturn(accessToken)
        whenever(cryptographyService.decrypt(accessToken)).thenReturn(decryptedAccessToken.toByteArray())
        whenever(repository.findRefreshTokenBy(userId)).thenReturn(refreshToken)
        whenever(cryptographyService.decrypt(refreshToken)).thenReturn(decryptedRefreshToken.toByteArray())
        whenever(dispatcher.sendEvent(any())).thenThrow(
            EventException(
                code = "code",
                parameters = emptyMap(),
                type = Unauthorized
            )
        )

        assertThatExceptionOfType(UnauthorizedUserException::class.java).isThrownBy {
            provider.getAccessTokenBy(userId)
        }

        verify(repository).findRefreshTokenBy(userId)
        verify(cryptographyService).decrypt(refreshToken)
    }

    @Test
    fun `should throw event exception for event exception different from unauthorized received from guiabolso`() {
        whenever(dateTimeProvider.now()).thenReturn(
            ZonedDateTime.of(2019, 4, 20, 16, 40, 0, 0, UTC)
        )

        EventContextHolder.setContext(
            context = EventContext(
                id = nextObject<String>(),
                flowId = nextObject<String>()
            )
        )

        val userId = nextObject<String>()
        val decryptedRefreshToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw"
        val refreshToken = EncryptedData("encrypted-$decryptedRefreshToken".toByteArray())
        val decryptedAccessToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjg0NDM3MTgzMX0.Pnrxw8d6w1wLuK7s_TTKG26vdmW7phpXL7CQewYkKmw"
        val accessToken = EncryptedData("encrypted-$decryptedAccessToken".toByteArray())

        whenever(cacheService.getData(any(), any(), any())).thenReturn(accessToken)
        whenever(cryptographyService.decrypt(accessToken)).thenReturn(decryptedAccessToken.toByteArray())
        whenever(repository.findRefreshTokenBy(userId)).thenReturn(refreshToken)
        whenever(cryptographyService.decrypt(refreshToken)).thenReturn(decryptedRefreshToken.toByteArray())
        whenever(dispatcher.sendEvent(any())).thenThrow(
            EventException(
                code = "code",
                parameters = emptyMap(),
                type = Generic
            )
        )

        assertThatExceptionOfType(EventException::class.java).isThrownBy {
            provider.getAccessTokenBy(userId)
        }

        verify(repository).findRefreshTokenBy(userId)
        verify(cryptographyService).decrypt(refreshToken)
    }

    companion object {
        private val DURATION = Duration.ofMinutes(5)
        private const val ONLY_IN_MEMORY = true
    }
}
