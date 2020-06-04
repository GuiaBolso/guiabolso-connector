package br.com.guiabolso.connector.token

import br.com.guiabolso.connector.common.cache.CacheService
import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.common.utils.isExpired
import br.com.guiabolso.connector.common.utils.toUTF8String
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.token.exception.ExpiredRefreshTokenException
import br.com.guiabolso.connector.token.exception.UnauthorizedUserException
import br.com.guiabolso.connector.token.model.Token
import br.com.guiabolso.connector.token.repository.TokenRepository
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.model.EventErrorType.Unauthorized
import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Duration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class AccessTokenProvider(
    private val repository: TokenRepository,
    @Qualifier("partnerEventDispatcher") private val dispatcher: EventDispatcher,
    private val cacheService: CacheService,
    private val cryptographyService: CryptographyService,
    private val dateTimeProvider: ZonedDateTimeProvider
) {

    fun getAccessTokenBy(userId: String): String {
        val accessToken = findAccessToken(userId) ?: throw UnauthorizedUserException()

        val decryptedAccessToken = cryptographyService.decrypt(accessToken).toUTF8String()

        if (JWT.decode(decryptedAccessToken).shouldBeRenewed())
            return getRefreshedAccessTokenBy(userId)

        return decryptedAccessToken
    }

    private fun findAccessToken(userId: String): EncryptedData? {
        return cacheService.getData(
            key = Token.getKey(userId),
            duration = Duration.ofMinutes(5),
            onlyInMemory = true
        ) ?: run {
            repository.findAccessTokenBy(userId)?.also {
                cacheService.putData(
                    key = Token.getKey(userId),
                    value = it,
                    duration = Duration.ofMinutes(5),
                    onlyInMemory = true
                )
            }
        }
    }

    private fun DecodedJWT.shouldBeRenewed(): Boolean {
        val thirtySecondsRemaining = dateTimeProvider.now().plusSeconds(30)
        return this.isExpired(reference = thirtySecondsRemaining)
    }

    private fun getRefreshedAccessTokenBy(userId: String): String {
        val encryptedRefreshToken = repository.findRefreshTokenBy(userId)
        if (encryptedRefreshToken == null) {
            logger.error("There isn't any refresh token associated for this userId $userId")
            throw UnauthorizedUserException()
        }

        val refreshToken = cryptographyService.decrypt(encryptedRefreshToken).toUTF8String()
        if (JWT.decode(refreshToken).isExpired(reference = dateTimeProvider.now())) {
            throw ExpiredRefreshTokenException()
        }

        return fetchNewAccessToken(refreshToken).also { accessToken ->
            cryptographyService.encrypt(accessToken.toByteArray()).let {
                repository.updateAccessToken(userId, accessToken = it)
                cacheService.putData(
                    key = Token.getKey(userId),
                    value = it,
                    duration = Duration.ofMinutes(5),
                    onlyInMemory = true
                )
            }
        }
    }

    private fun fetchNewAccessToken(refreshToken: String): String {
        val event = EventBuilder.event {
            name = "oauth:refresh:token:exchange"
            version = 1
            payload = mapOf("refreshToken" to refreshToken)
        }

        return try {
            dispatcher.sendEvent(event).payloadAs(TokenRefreshResponse::class.java).accessToken
        } catch (e: EventException) {
            logger.error("Dispatcher result in error with type ${e.type} and code ${e.code}")
            if (e.type == Unauthorized) throw UnauthorizedUserException(e)
            else throw e
        }
    }

    private data class TokenRefreshResponse(
        val accessToken: String
    )

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AccessTokenProvider::class.java)
    }
}
