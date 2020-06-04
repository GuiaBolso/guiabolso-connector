package br.com.guiabolso.connector.handlers

import br.com.guiabolso.connector.auth.AuthenticationService
import br.com.guiabolso.connector.common.code.EventsErrorCode.UNKNOWN_DISPATCH_ERROR
import br.com.guiabolso.connector.common.tracking.Tracer.executeAsync
import br.com.guiabolso.connector.datapackage.model.DataPackage
import br.com.guiabolso.connector.event.cache.CachedEventDispatcher
import br.com.guiabolso.connector.event.cache.EventCacheService
import br.com.guiabolso.connector.event.cache.withCacheUsage
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.misc.requiredString
import br.com.guiabolso.connector.event.model.EventIdentifier
import br.com.guiabolso.connector.handlers.models.Uts
import br.com.guiabolso.connector.handlers.models.UtsVariable
import br.com.guiabolso.events.builder.EventBuilder.Companion.responseFor
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import br.com.guiabolso.events.server.handler.EventHandler
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MergingEventHandler(
    private val dataPackage: DataPackage,
    private val eventDispatcher: CachedEventDispatcher,
    private val eventAuthenticationService: AuthenticationService,
    private val eventCacheService: EventCacheService
) : EventHandler {

    override val eventName = dataPackage.publish.name
    override val eventVersion = dataPackage.publish.version

    override fun handle(event: RequestEvent): ResponseEvent {
        val userId = event.identity.requiredString("userId")
        val eventIdentifier = EventIdentifier(event.name, event.version)

        if (eventCacheService.shouldUseCachedEvent(eventIdentifier)) {
            eventCacheService.getCachedEvent(userId, eventIdentifier)?.let {
                return it.withCacheUsage(event, true)
            }
        }

        val authenticatedEvent = eventAuthenticationService.authenticate(event)
        val dispatchedTasks = dataPackage.sources.map { source ->
            val future = executeAsync(executor) {
                dispatchEvent(authenticatedEvent, source.eventName, source.eventVersion).payloadAs<Uts>()
            }
            Pair(source, future)
        }

        val variables = mutableSetOf<UtsVariable>()
        for (task in dispatchedTasks) {
            val (source, future) = task
            try {
                val dispatchResponse = future.get().variables ?: emptyList()

                variables.add(UtsVariable(key = source.statusKey, value = SUCCESS, type = "STRING"))
                variables.addAll(dispatchResponse)
            } catch (e: ExecutionException) {
                if (eventCacheService.shouldUseCachedEventOnFailure(eventIdentifier)) {
                    eventCacheService.getCachedEvent(userId, eventIdentifier)?.let {
                        logger.info("Use cached event due to CacheUsagePolicy=ONLY_ON_FAILURES", e.cause)
                        return it.withCacheUsage(event, true)
                    }
                    logger.warn(
                        """
                            Request have been failed and CacheUsagePolicy=ONLY_ON_FAILURES
                            is enabled but there isn't any cache entry to use for $eventIdentifier for userId=$userId!"
                        """
                    )
                }
                logger.error("Fail on dispatch $eventIdentifier", e.cause)
                variables.add(UtsVariable(key = source.statusKey, value = resolveError(e.cause), type = "STRING"))
            }
        }

        val response = responseFor(authenticatedEvent) {
            payload = mapOf("variables" to variables)
        }

        if (eventCacheService.shouldCacheEvent(eventIdentifier)) {
            eventCacheService.cacheEvent(userId, eventIdentifier, response)
        }

        return response.withCacheUsage(authenticatedEvent, false)
    }

    private fun dispatchEvent(mainEvent: RequestEvent, eventName: String, eventVersion: Int): ResponseEvent {
        return eventDispatcher.sendEvent(
            mainEvent.copy(
                name = eventName,
                version = eventVersion
            )
        )
    }

    private fun resolveError(e: Throwable?) = when (e) {
        is EventException -> "$ERROR: ${e.code}"
        else -> "$ERROR: $UNKNOWN_DISPATCH_ERROR"
    }

    companion object {
        private const val ERROR = "ERROR"
        private const val SUCCESS = "SUCCESS"

        private val executor: ExecutorService = Executors.newCachedThreadPool()
        private val logger: Logger = LoggerFactory.getLogger(MergingEventHandler::class.java)
    }
}
