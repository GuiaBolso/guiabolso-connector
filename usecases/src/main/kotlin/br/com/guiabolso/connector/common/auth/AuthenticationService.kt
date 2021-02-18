package br.com.guiabolso.connector.common.auth

import br.com.guiabolso.events.model.RequestEvent

interface AuthenticationService {
    fun authenticate(requestEvent: RequestEvent): RequestEvent
}
