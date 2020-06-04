package br.com.guiabolso.connector.event.partner

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.integration.EventBrokerService
import br.com.guiabolso.connector.event.misc.authenticatedAsClient
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import org.springframework.stereotype.Service

@Service
class PartnerEventDispatcher(
    config: ConfigService,
    private val clientCredentials: ClientCredentials,
    private val eventBrokerService: EventBrokerService
) : EventDispatcher {

    private val uri = config.getRequiredString("kasbah.url") + config.getRequiredString("partner.route")

    override fun sendEvent(event: RequestEvent): ResponseEvent {
        return eventBrokerService.sendEvent(uri, event.authenticatedAsClient(clientCredentials))
    }
}
