package br.com.guiabolso.connector.proxy

import br.com.guiabolso.events.server.handler.EventHandler
import br.com.guiabolso.events.server.handler.EventHandlerDiscovery

class CompositeEventHandlerDiscovery(
    private val discoveries: List<EventHandlerDiscovery>
) : EventHandlerDiscovery {

    override fun eventHandlerFor(eventName: String, eventVersion: Int): EventHandler? {
        for (discovery in discoveries) {
            val handler = discovery.eventHandlerFor(eventName, eventVersion)
            if (handler != null) return handler
        }

        return null
    }
}
