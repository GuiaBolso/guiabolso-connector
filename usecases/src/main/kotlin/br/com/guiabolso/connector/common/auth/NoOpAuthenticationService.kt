package br.com.guiabolso.connector.common.auth

import br.com.guiabolso.events.model.RequestEvent

object NoOpAuthenticationService : AuthenticationService {
    override fun authenticate(requestEvent: RequestEvent) = requestEvent
}
