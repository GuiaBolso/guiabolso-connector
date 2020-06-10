package br.com.guiabolso.connector.token

import br.com.guiabolso.connector.common.cache.CacheService
import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.connector.helper.createJWT
import br.com.guiabolso.connector.token.exception.ExpiredAuthorizationCodeException
import br.com.guiabolso.connector.token.model.Token
import br.com.guiabolso.connector.token.repository.TokenRepository
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.context.EventContext
import br.com.guiabolso.events.context.EventContextHolder
import br.com.guiabolso.events.model.RequestEvent
import com.google.gson.JsonObject
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import java.time.Duration
import java.time.ZonedDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TokenExchangeServiceTest {

    private lateinit var repository: TokenRepository
    private lateinit var dispatcher: EventDispatcher
    private lateinit var cacheService: CacheService
    private lateinit var cryptographyService: CryptographyService
    private lateinit var service: TokenExchangeService
    private lateinit var dateTimeProvider: ZonedDateTimeProvider

    @BeforeEach
    fun setUp() {
        repository = mock()
        dispatcher = mock()
        cacheService = mock()
        cryptographyService = mock()
        dateTimeProvider = mock()

        service = TokenExchangeService(repository, dispatcher, cacheService, cryptographyService, dateTimeProvider)
    }

    @Test
    fun `should exchange tokens`() {
        EventContextHolder.setContext(
            context = EventContext(
                id = nextObject<String>(),
                flowId = nextObject<String>()
            )
        )

        val userId = nextObject<String>()
        val authorizationCode = createJWT(now(), now().plusMinutes(1))
        val key = Token.getKey(userId)
        val decryptedAccessToken = nextObject<String>()
        val accessToken = nextObject<EncryptedData>()
        val decryptedRefreshToken = nextObject<String>()
        val refreshToken = nextObject<EncryptedData>()

        val responseEvent = EventBuilder.responseFor(buildEvent()) {}.copy(
            payload = JsonObject().apply {
                addProperty("accessToken", decryptedAccessToken)
                addProperty("refreshToken", decryptedRefreshToken)
            }
        )

        whenever(dateTimeProvider.now()).thenReturn(now())
        whenever(cryptographyService.encrypt(decryptedAccessToken.toByteArray())).thenReturn(accessToken)
        whenever(cryptographyService.encrypt(decryptedRefreshToken.toByteArray())).thenReturn(refreshToken)
        whenever(dispatcher.sendEvent(any())).thenReturn(responseEvent)

        service.exchangeTokens(userId, authorizationCode)

        argumentCaptor<RequestEvent>().run {
            verify(dispatcher).sendEvent(capture())
            assertThat(firstValue.payload.asJsonObject["code"].asString).isEqualTo(authorizationCode)
        }

        verify(cryptographyService).encrypt(decryptedAccessToken.toByteArray())
        verify(cryptographyService).encrypt(decryptedRefreshToken.toByteArray())
        verify(cacheService).putData(key, accessToken, DURATION, ONLY_IN_MEMORY)
        verify(repository).putToken(userId, accessToken, refreshToken)
    }

    @Test
    fun `should rise when authorization code is expired`() {
        val now = now()
        whenever(dateTimeProvider.now()).thenReturn(now)
        val expiredToken = createJWT(issuedAt = now.minusMinutes(1), expireAt = now.minusSeconds(1))

        assertThatExceptionOfType(ExpiredAuthorizationCodeException::class.java)
            .isThrownBy {
                service.exchangeTokens(userId = nextObject(), authorizationCode = expiredToken)
            }
    }

    companion object {
        private val DURATION = Duration.ofMinutes(5)
        private const val ONLY_IN_MEMORY = true
    }
}
