package br.com.guiabolso.connector.handlers.oauth

import br.com.guiabolso.connector.event.misc.requiredString
import br.com.guiabolso.connector.user.UserAuthorizationService
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import br.com.guiabolso.events.server.handler.EventHandler
import org.springframework.stereotype.Component

@Component
class UserAuthorizationEventHandler(
    private val userAuthorizationService: UserAuthorizationService
) : EventHandler {
    override val eventName = "is:user:authorized"
    override val eventVersion = 1

    override fun handle(event: RequestEvent): ResponseEvent {
        val userId = event.identity.requiredString("userId")
        return EventBuilder.responseFor(event) {
            payload = userAuthorizationService.isUserAuthorized(userId)
        }
    }
}
