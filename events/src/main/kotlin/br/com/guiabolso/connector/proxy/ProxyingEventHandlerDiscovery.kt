package br.com.guiabolso.connector.proxy

import br.com.guiabolso.connector.auth.AuthenticationService
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.wrapper.LoggingEventHandlerWrapper
import br.com.guiabolso.events.server.handler.EventHandler
import br.com.guiabolso.events.server.handler.EventHandlerDiscovery

class ProxyingEventHandlerDiscovery(
    configService: ConfigService,
    private val eventDispatcher: EventDispatcher,
    private val eventAuthenticationService: AuthenticationService
) : EventHandlerDiscovery {

    private val logEnabled = configService.getBoolean("event.log.startAndFinish", true)

    override fun eventHandlerFor(eventName: String, eventVersion: Int): EventHandler? {
        val proxyingEventHandler = ProxyingEventHandler(
            eventName = eventName,
            eventVersion = eventVersion,
            dispatcher = eventDispatcher,
            eventAuthenticationService = eventAuthenticationService
        )
        return if (logEnabled) {
            LoggingEventHandlerWrapper(proxyingEventHandler)
        } else proxyingEventHandler
    }
}
