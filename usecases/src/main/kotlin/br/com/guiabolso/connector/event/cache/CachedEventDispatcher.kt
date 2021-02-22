package br.com.guiabolso.connector.event.cache

import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedPolicy
import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.misc.requiredString
import br.com.guiabolso.connector.event.model.EventIdentifier
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class CachedEventDispatcher(
    @Qualifier("userEventDispatcher") private val dispatcher: EventDispatcher,
    private val eventCacheService: EventCacheService,
    private val redirectOnUnauthorizedService: RedirectOnUnauthorizedService
) : EventDispatcher {

    override fun sendEvent(event: RequestEvent): ResponseEvent {
        val userId = event.identity.requiredString("userId")
        val eventIdentifier = EventIdentifier(event.name, event.version)

        if (eventCacheService.shouldUseCachedEvent(eventIdentifier)) {
            eventCacheService.getCachedEvent(userId, eventIdentifier)?.let {
                return it.withCacheUsage(event, true)
            }
        }

        try {
            val response = dispatcher.sendEvent(event)

            if (eventCacheService.shouldCacheEvent(eventIdentifier)) {
                eventCacheService.cacheEvent(userId, eventIdentifier, response)
            }

            return response.withCacheUsage(event, false)
        } catch (e: Exception) {
            redirectOnUnauthorizedService.maybeRedirectFor(event, e, REQUIRED_POLICY)

            if (eventCacheService.shouldUseCachedEventOnFailure(eventIdentifier)) {
                eventCacheService.getCachedEvent(userId, eventIdentifier)?.let {
                    return it.withCacheUsage(event, true)
                }
            }

            throw e
        }
    }

    companion object {
        private val REQUIRED_POLICY = RedirectOnUnauthorizedPolicy.USER_EVENTS
    }
}
