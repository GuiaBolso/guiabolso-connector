package br.com.guiabolso.connector.event.cache

import br.com.guiabolso.connector.common.cache.CacheService
import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.utils.toUTF8String
import br.com.guiabolso.connector.event.model.CacheUsagePolicy.ALWAYS
import br.com.guiabolso.connector.event.model.CacheUsagePolicy.NEVER
import br.com.guiabolso.connector.event.model.CacheUsagePolicy.ONLY_ON_FAILURES
import br.com.guiabolso.connector.event.model.EventCacheConfig
import br.com.guiabolso.connector.event.model.EventIdentifier
import br.com.guiabolso.connector.event.model.EventVersion.AllVersions
import br.com.guiabolso.connector.event.model.EventVersion.ExactVersion
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.json.MapperHolder.mapper
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import com.google.gson.JsonPrimitive
import java.time.Duration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventCacheService(
    private val cacheService: CacheService,
    private val cryptographyService: CryptographyService,
    eventCachingConfig: List<EventCacheConfig>
) {

    private val cacheConfig = eventCachingConfig.map {
        Pair(it.eventName, it.version) to it
    }.toMap()

    fun shouldCacheEvent(eventIdentifier: EventIdentifier): Boolean {
        return getConfig(eventIdentifier).cacheUsagePolicy != NEVER
    }

    fun shouldUseCachedEvent(eventIdentifier: EventIdentifier): Boolean {
        return getConfig(eventIdentifier).cacheUsagePolicy == ALWAYS
    }

    fun shouldUseCachedEventOnFailure(eventIdentifier: EventIdentifier): Boolean {
        return getConfig(eventIdentifier).cacheUsagePolicy == ONLY_ON_FAILURES
    }

    fun getCachedEvent(userId: String, eventIdentifier: EventIdentifier): ResponseEvent? {
        return if (shouldCacheEvent(eventIdentifier)) {
            cacheService.getData(
                key = userKey(userId, eventIdentifier),
                duration = getConfig(eventIdentifier).duration,
                onlyInMemory = false
            )?.let {
                logger.info("Using cached event with identifier $eventIdentifier")
                mapper.fromJson(cryptographyService.decrypt(it).toUTF8String(), ResponseEvent::class.java)
            }
        } else null
    }

    fun cacheEvent(userId: String, eventIdentifier: EventIdentifier, response: ResponseEvent) {
        if (shouldCacheEvent(eventIdentifier)) {
            cacheService.putData(
                key = userKey(userId, eventIdentifier),
                value = cryptographyService.encrypt(mapper.toJson(response).toByteArray()),
                duration = getConfig(eventIdentifier).duration,
                onlyInMemory = false
            )
        }
    }

    private fun userKey(userId: String, event: EventIdentifier) = "event-$userId-$event"

    private fun getConfig(event: EventIdentifier): EventCacheConfig {
        return cacheConfig[Pair(event.name, ExactVersion(event.version))]
            ?: cacheConfig[Pair(event.name, AllVersions)]
            ?: EventCacheConfig(
                eventName = event.name,
                version = ExactVersion(event.version),
                duration = Duration.ZERO,
                cacheUsagePolicy = NEVER
            )
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(EventCacheService::class.java)
    }
}

fun ResponseEvent.withCacheUsage(event: RequestEvent, isCached: Boolean) = EventBuilder.responseFor(event) {
    payload = this@withCacheUsage.payload
    identity = this@withCacheUsage.identity
    auth = this@withCacheUsage.auth
    metadata = this@withCacheUsage.metadata.deepCopy().apply {
        add("cached", JsonPrimitive(isCached))
    }
}
