package br.com.guiabolso.connector.proxy

import br.com.guiabolso.connector.common.auth.AuthenticationService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import br.com.guiabolso.events.server.handler.EventHandler

class ProxyingEventHandler(
    override val eventName: String,
    override val eventVersion: Int,
    private val dispatcher: EventDispatcher,
    private val eventAuthenticationService: AuthenticationService
) : EventHandler {

    override fun handle(event: RequestEvent): ResponseEvent {
        val authenticatedEvent = eventAuthenticationService.authenticate(event)
        return dispatcher.sendEvent(authenticatedEvent)
    }
}
