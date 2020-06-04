package br.com.guiabolso.connector.auth

import br.com.guiabolso.events.model.RequestEvent

object NoOpAuthenticationService : AuthenticationService {
    override fun authenticate(requestEvent: RequestEvent) = requestEvent
}
