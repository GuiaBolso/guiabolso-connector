package br.com.guiabolso.connector.event.partner

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedPolicy
import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedService
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.integration.EventBrokerService
import br.com.guiabolso.connector.event.misc.authenticatedAsClient
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import java.lang.Exception
import org.springframework.stereotype.Service

@Service
class PartnerEventDispatcher(
    config: ConfigService,
    private val clientCredentials: ClientCredentials,
    private val eventBrokerService: EventBrokerService,
    private val redirectOnUnauthorizedService: RedirectOnUnauthorizedService
) : EventDispatcher {

    private val uri = config.getRequiredString("kasbah.url") + config.getRequiredString("partner.route")

    override fun sendEvent(event: RequestEvent): ResponseEvent {
        return try {
            eventBrokerService.sendEvent(uri, event.authenticatedAsClient(clientCredentials))
        } catch (e: Exception) {
            redirectOnUnauthorizedService.maybeRedirectFor(event, e, REQUIRED_POLICY)
            throw e
        }
    }

    companion object {
        private val REQUIRED_POLICY = RedirectOnUnauthorizedPolicy.PARTNER_EVENTS
    }
}
