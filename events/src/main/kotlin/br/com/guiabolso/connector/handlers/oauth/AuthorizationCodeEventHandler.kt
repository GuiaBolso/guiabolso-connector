package br.com.guiabolso.connector.handlers.oauth

import br.com.guiabolso.connector.event.misc.requiredString
import br.com.guiabolso.connector.token.TokenExchangeService
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import br.com.guiabolso.events.server.handler.EventHandler
import org.springframework.stereotype.Component

@Component
class AuthorizationCodeEventHandler(
    private val tokenExchangeService: TokenExchangeService
) : EventHandler {
    override val eventName = "authorization:code"
    override val eventVersion = 1

    override fun handle(event: RequestEvent): ResponseEvent {
        val userId = event.identity.requiredString("userId")
        val authorizationCode = event.payload.requiredString("authorizationCode")

        tokenExchangeService.exchangeTokens(userId, authorizationCode)

        return EventBuilder.responseFor(event) {
            payload = emptyMap<String, String>()
        }
    }
}
