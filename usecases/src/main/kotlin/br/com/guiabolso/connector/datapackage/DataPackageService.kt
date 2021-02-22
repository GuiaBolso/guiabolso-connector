package br.com.guiabolso.connector.datapackage

import br.com.guiabolso.connector.common.auth.AuthenticationService
import br.com.guiabolso.connector.common.tracking.Tracer
import br.com.guiabolso.connector.datapackage.model.DataPackage
import br.com.guiabolso.connector.datapackage.model.PackageSource
import br.com.guiabolso.connector.datapackage.model.Uts
import br.com.guiabolso.connector.datapackage.model.UtsVariable
import br.com.guiabolso.connector.event.cache.CachedEventDispatcher
import br.com.guiabolso.connector.event.cache.EventCacheService
import br.com.guiabolso.connector.event.cache.withCacheUsage
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.connector.event.misc.requiredString
import br.com.guiabolso.connector.event.model.EventIdentifier
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DataPackageService(
    private val eventCacheService: EventCacheService,
    private val eventDispatcher: CachedEventDispatcher,
    private val eventAuthenticationService: AuthenticationService
) {

    fun handleDataPackage(dataPackage: DataPackage, originEvent: RequestEvent): ResponseEvent {
        val userId = originEvent.identity.requiredString("userId")
        val eventIdentifier = EventIdentifier(originEvent.name, originEvent.version)

        findCachedResponse(userId, eventIdentifier)?.let { response ->
            return response.withCacheUsage(originEvent, true)
        }

        return requestForDataPackage(dataPackage, eventAuthenticationService.authenticate(originEvent))
    }

    private fun requestForDataPackage(dataPackage: DataPackage, event: RequestEvent): ResponseEvent {
        val userId = event.identity.requiredString("userId")
        val eventIdentifier = EventIdentifier(event.name, event.version)

        val result = dataPackage
            .sources
            .map { source -> dispatchEvent(event, source) }
            .map { handleTask(it) }

        if (result.any { (_, success) -> !success } && eventCacheService.shouldUseCachedEventOnFailure(eventIdentifier)) {
            eventCacheService.getCachedEvent(userId, eventIdentifier)?.let {
                logger.info("Use cached event due to CacheUsagePolicy=ONLY_ON_FAILURES for $eventIdentifier")
                return it.withCacheUsage(event, true)
            }
            logger.warn(
                """
                    Request have been failed and CacheUsagePolicy=ONLY_ON_FAILURES
                    is enabled but there isn't any cache entry to use for $eventIdentifier for userId=$userId!"
                """
            )
        }

        val variables = result.flatMapTo(mutableSetOf()) { it.first }
        val response = EventBuilder
            .responseFor(event) { payload = mapOf("variables" to variables) }
            .also { maybeCache(userId, eventIdentifier, it) }
        return response.withCacheUsage(event, false)
    }

    private fun findCachedResponse(userId: String, eventIdentifier: EventIdentifier): ResponseEvent? {
        return if (eventCacheService.shouldUseCachedEvent(eventIdentifier))
            eventCacheService.getCachedEvent(userId, eventIdentifier)
        else null
    }

    private fun dispatchEvent(
        event: RequestEvent,
        source: PackageSource
    ): Pair<PackageSource, Future<Uts>> {

        val future = Tracer.executeAsync(executor) {
            eventDispatcher
                .sendEvent(event.copy(name = source.eventName, version = source.eventVersion))
                .payloadAs<Uts>()
        }
        return Pair(source, future)
    }

    private fun handleTask(task: Pair<PackageSource, Future<Uts>>): Pair<Set<UtsVariable>, Boolean> {
        val (source, future) = task
        val variables = mutableSetOf<UtsVariable>()

        return try {
            val dispatchResponse = future.get().variables ?: emptyList()

            variables.add(UtsVariable(key = source.statusKey, value = SUCCESS, type = "STRING"))
            variables.addAll(dispatchResponse)
            Pair(variables, true)
        } catch (e: ExecutionException) {
            logger.error("Fail on dispatch $source", e)

            if (e.cause is RedirectException) throw e.cause!!
            variables.add(UtsVariable(key = source.statusKey, value = resolveError(e.cause), type = "STRING"))
            Pair(variables, false)
        }
    }

    private fun maybeCache(userId: String, eventIdentifier: EventIdentifier, response: ResponseEvent) {
        if (eventCacheService.shouldCacheEvent(eventIdentifier)) {
            eventCacheService.cacheEvent(userId, eventIdentifier, response)
        }
    }

    private fun resolveError(e: Throwable?) = when (e) {
        is EventException -> "$ERROR: ${e.code}"
        else -> "$ERROR: UNKNOWN_DISPATCH_ERROR"
    }

    companion object {
        private const val ERROR = "ERROR"
        private const val SUCCESS = "SUCCESS"

        private val executor: ExecutorService = Executors.newCachedThreadPool()
        private val logger = LoggerFactory.getLogger(DataPackageService::class.java)
    }
}
