package br.com.guiabolso.connector.handlers

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.event.misc.authenticatedAsClient
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.server.handler.EventHandler

class PartnerProxyingEventHandler(
    private val eventHandler: EventHandler,
    private val clientCredentials: ClientCredentials
) : EventHandler {

    override val eventName = eventHandler.eventName
    override val eventVersion = eventHandler.eventVersion

    override fun handle(event: RequestEvent) = eventHandler.handle(event.authenticatedAsClient(clientCredentials))
}
