package br.com.guiabolso.connector.event.user

import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.integration.EventBrokerService
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import org.springframework.stereotype.Service

@Service
class UserEventDispatcher(
    config: ConfigService,
    private val eventBrokerService: EventBrokerService
) : EventDispatcher {

    private val uri = config.getRequiredString("kasbah.url") + config.getRequiredString("user.route")

    override fun sendEvent(event: RequestEvent): ResponseEvent {
        return eventBrokerService.sendEvent(uri, event)
    }
}
