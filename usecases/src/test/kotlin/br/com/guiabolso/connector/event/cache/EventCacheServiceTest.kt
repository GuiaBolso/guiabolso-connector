package br.com.guiabolso.connector.event.cache

import br.com.guiabolso.connector.common.cache.CacheService
import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.connector.event.model.CacheUsagePolicy.ALWAYS
import br.com.guiabolso.connector.event.model.CacheUsagePolicy.NEVER
import br.com.guiabolso.connector.event.model.CacheUsagePolicy.ONLY_ON_FAILURES
import br.com.guiabolso.connector.event.model.EventCacheConfig
import br.com.guiabolso.connector.event.model.EventIdentifier
import br.com.guiabolso.connector.event.model.EventVersion.AllVersions
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.json.MapperHolder.mapper
import br.com.guiabolso.events.model.ResponseEvent
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EventCacheServiceTest {

    private lateinit var cacheService: CacheService
    private lateinit var cryptographyService: CryptographyService
    private lateinit var service: EventCacheService

    private fun setUp(eventCachingConfig: List<EventCacheConfig>) {
        cacheService = mock()
        cryptographyService = mock()

        service = EventCacheService(
            cacheService,
            cryptographyService,
            eventCachingConfig
        )
    }

    @Test
    fun `should return true for shouldCacheEvent`() {
        listOf(
            nextObject<EventCacheConfig>().copy(cacheUsagePolicy = ALWAYS, version = AllVersions),
            nextObject<EventCacheConfig>().copy(cacheUsagePolicy = ONLY_ON_FAILURES, version = AllVersions)
        ).forEach {
            setUp(listOf(it))

            val eventIdentifier = EventIdentifier(name = it.eventName, version = nextObject())

            val actual = service.shouldCacheEvent(eventIdentifier)

            assertThat(actual).isTrue()
        }

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return false for shouldCacheEvent due to its policy`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = NEVER, version = AllVersions)
            .also { setUp(listOf(it)) }

        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())

        val actual = service.shouldCacheEvent(eventIdentifier)

        assertThat(actual).isFalse()

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return false for shouldCacheEvent when event is not present in data cache`() {
        setUp(emptyList())

        val actual = service.shouldCacheEvent(nextObject())

        assertThat(actual).isFalse()

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return true for shouldUseCachedEvent`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = ALWAYS, version = AllVersions)
            .also { setUp(listOf(it)) }

        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())

        val actual = service.shouldUseCachedEvent(eventIdentifier)

        assertThat(actual).isTrue()

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return false for shouldUseCachedEvent due to its policy`() {
        listOf(
            nextObject<EventCacheConfig>().copy(cacheUsagePolicy = ONLY_ON_FAILURES, version = AllVersions),
            nextObject<EventCacheConfig>().copy(cacheUsagePolicy = NEVER, version = AllVersions)
        ).forEach {
            setUp(listOf(it))

            val eventIdentifier = EventIdentifier(name = it.eventName, version = nextObject())

            val actual = service.shouldUseCachedEvent(eventIdentifier)

            assertThat(actual).isFalse()
        }

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return false for shouldUseCachedEvent when event is not present in data cache`() {
        setUp(emptyList())

        val actual = service.shouldUseCachedEvent(nextObject())

        assertThat(actual).isFalse()

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return true for shouldUseCachedEventOnFailure`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = ONLY_ON_FAILURES, version = AllVersions)
            .also { setUp(listOf(it)) }

        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())

        val actual = service.shouldUseCachedEventOnFailure(eventIdentifier)

        assertThat(actual).isTrue()

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return false for shouldUseCachedEventOnFailure due to its policy`() {
        listOf(
            nextObject<EventCacheConfig>().copy(cacheUsagePolicy = ALWAYS, version = AllVersions),
            nextObject<EventCacheConfig>().copy(cacheUsagePolicy = NEVER, version = AllVersions)
        ).forEach {
            setUp(listOf(it))

            val eventIdentifier = EventIdentifier(name = it.eventName, version = nextObject())

            val actual = service.shouldUseCachedEventOnFailure(eventIdentifier)

            assertThat(actual).isFalse()
        }

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return false for shouldUseCachedEventOnFailure when event is not present in data cache`() {
        setUp(emptyList())

        val actual = service.shouldUseCachedEventOnFailure(nextObject())

        assertThat(actual).isFalse()

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return null when event is not cached`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = ALWAYS, version = AllVersions)
            .also { setUp(listOf(it)) }

        val userId = nextObject<String>()

        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())

        val userKey = "event-$userId-$eventIdentifier"

        whenever(cacheService.getData(userKey, eventCacheConfig.duration, ONLY_IN_MEMORY)).thenReturn(null)

        val actual = service.getCachedEvent(userId, eventIdentifier)

        assertThat(actual).isNull()

        verify(cacheService).getData(userKey, eventCacheConfig.duration, ONLY_IN_MEMORY)
        verifyZeroInteractions(cryptographyService)
    }

    @Test
    fun `should return null due to event's policy`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = NEVER, version = AllVersions)
            .also { setUp(listOf(it)) }

        val userId = nextObject<String>()

        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())

        val actual = service.getCachedEvent(userId, eventIdentifier)

        assertThat(actual).isNull()

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should return cached response`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = ALWAYS, version = AllVersions)
            .also { setUp(listOf(it)) }

        val userId = nextObject<String>()
        val responseEvent = EventBuilder.responseFor(buildEvent()) {}
        val data = mapper.toJson(responseEvent)
        val encryptedData = EncryptedData(data.toByteArray())

        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())

        val userKey = "event-$userId-$eventIdentifier"

        whenever(cacheService.getData(userKey, eventCacheConfig.duration, ONLY_IN_MEMORY)).thenReturn(encryptedData)
        whenever(cryptographyService.decrypt(encryptedData)).thenReturn(data.toByteArray())

        val actual = service.getCachedEvent(userId, eventIdentifier)

        assertThat(actual).isEqualTo(responseEvent)

        verify(cacheService).getData(userKey, eventCacheConfig.duration, ONLY_IN_MEMORY)
        verify(cryptographyService).decrypt(encryptedData)
    }

    @Test
    fun `should not cache event due to its policy`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = NEVER, version = AllVersions)
            .also { setUp(listOf(it)) }

        val userId = nextObject<String>()
        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())
        val responseEvent = nextObject<ResponseEvent>()

        service.cacheEvent(userId, eventIdentifier, responseEvent)

        verifyZeroInteractions(cacheService, cryptographyService)
    }

    @Test
    fun `should cache event response`() {
        val eventCacheConfig = nextObject<EventCacheConfig>()
            .copy(cacheUsagePolicy = ALWAYS, version = AllVersions)
            .also { setUp(listOf(it)) }

        val userId = nextObject<String>()
        val responseEvent = EventBuilder.responseFor(buildEvent()) {}
        val data = mapper.toJson(responseEvent)
        val encryptedData = EncryptedData(data.toByteArray())

        val eventIdentifier = EventIdentifier(name = eventCacheConfig.eventName, version = nextObject())

        val userKey = "event-$userId-$eventIdentifier"

        whenever(cryptographyService.encrypt(data.toByteArray())).thenReturn(encryptedData)

        service.cacheEvent(userId, eventIdentifier, responseEvent)

        verify(cryptographyService).encrypt(data.toByteArray())
        verify(cacheService).putData(userKey, encryptedData, eventCacheConfig.duration, ONLY_IN_MEMORY)
    }

    companion object {
        private const val ONLY_IN_MEMORY = false
    }
}
