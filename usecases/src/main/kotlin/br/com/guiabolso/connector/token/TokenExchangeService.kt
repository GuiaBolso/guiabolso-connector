package br.com.guiabolso.connector.token

import br.com.guiabolso.connector.common.cache.CacheService
import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.common.utils.isExpired
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.token.exception.ExpiredAuthorizationCodeException
import br.com.guiabolso.connector.token.model.Token
import br.com.guiabolso.connector.token.repository.TokenRepository
import br.com.guiabolso.events.builder.EventBuilder
import com.auth0.jwt.JWT
import java.time.Duration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class TokenExchangeService(
    private val repository: TokenRepository,
    @Qualifier("partnerEventDispatcher") private val dispatcher: EventDispatcher,
    private val cacheService: CacheService,
    private val cryptographyService: CryptographyService,
    private val dateTimeProvider: ZonedDateTimeProvider
) {

    fun exchangeTokens(userId: String, authorizationCode: String) {
        authorizationCode.ensureIsNotExpired()

        val event = EventBuilder.event {
            name = "oauth:tokens:exchange"
            version = 1
            payload = mapOf("code" to authorizationCode)
        }

        val response = dispatcher.sendEvent(event).payloadAs(Response::class.java)

        val accessToken = cryptographyService.encrypt(response.accessToken.toByteArray())
        val refreshToken = cryptographyService.encrypt(response.refreshToken.toByteArray())

        cacheService.putData(
            key = Token.getKey(userId),
            value = accessToken,
            duration = Duration.ofMinutes(5),
            onlyInMemory = true
        )

        repository.putToken(userId, accessToken, refreshToken)
    }

    private fun String.ensureIsNotExpired() {
        val decodedJwt = JWT.decode(this)
        if (decodedJwt.isExpired(reference = dateTimeProvider.now())) {
            val tokenMetadata = mapOf(
                "id" to decodedJwt.id,
                "issuedAt" to decodedJwt.issuedAt,
                "expiredAt" to decodedJwt.expiresAt
            )
            logger.error("Expired AuthorizationCode, tokenMetadata=$tokenMetadata")
            throw ExpiredAuthorizationCodeException(mapOf("token" to this))
        }
    }

    private data class Response(
        val accessToken: String,
        val refreshToken: String
    )

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(TokenExchangeService::class.java)
    }
}
