package br.com.guiabolso.connector.auth

import br.com.guiabolso.events.model.RequestEvent

interface AuthenticationService {
    fun authenticate(requestEvent: RequestEvent): RequestEvent
}
