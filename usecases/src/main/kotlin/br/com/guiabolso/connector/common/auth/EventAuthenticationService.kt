package br.com.guiabolso.connector.common.auth

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.connector.event.misc.authenticatedAsUser
import br.com.guiabolso.connector.event.misc.requiredString
import br.com.guiabolso.connector.token.AccessTokenProvider
import br.com.guiabolso.connector.token.exception.ExpiredRefreshTokenException
import br.com.guiabolso.events.model.RedirectPayload
import br.com.guiabolso.events.model.RequestEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventAuthenticationService(
    private val accessTokenProvider: AccessTokenProvider,
    private val clientCredentials: ClientCredentials,
    configService: ConfigService
) : AuthenticationService {
    private val gbConnectUrl = configService.getRequiredString("gbconnect.url")

    override fun authenticate(requestEvent: RequestEvent): RequestEvent {
        return try {
            val userId = requestEvent.identity.requiredString("userId")
            val accessToken = accessTokenProvider.getAccessTokenBy(userId)
            requestEvent.authenticatedAsUser(clientCredentials, accessToken)
        } catch (e: ExpiredRefreshTokenException) {
            logger.info("Send a redirect caused by ExpiredRefreshToken", e)
            throw RedirectException(requestEvent, RedirectPayload(gbConnectUrl), e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EventAuthenticationService::class.java)
    }
}
