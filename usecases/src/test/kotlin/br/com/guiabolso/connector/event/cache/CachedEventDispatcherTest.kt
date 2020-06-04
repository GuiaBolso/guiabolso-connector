package br.com.guiabolso.connector.event.cache

import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.exception.MissingRequiredParameterException
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.connector.event.model.EventIdentifier
import br.com.guiabolso.events.builder.EventBuilder
import com.google.gson.JsonObject
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CachedEventDispatcherTest {

    private lateinit var eventDispatcher: EventDispatcher
    private lateinit var eventCacheService: EventCacheService
    private lateinit var dispatcher: CachedEventDispatcher

    @BeforeEach
    fun setUp() {
        eventDispatcher = mock()
        eventCacheService = mock()
        dispatcher = CachedEventDispatcher(eventDispatcher, eventCacheService)
    }

    @Test
    fun `should throw exception when userId is null`() {
        val event = buildEvent()

        assertThatExceptionOfType(MissingRequiredParameterException::class.java).isThrownBy {
            dispatcher.sendEvent(event)
        }

        verifyZeroInteractions(eventDispatcher, eventCacheService)
    }

    @Test
    fun `should return from cache`() {
        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)
        val responseEvent = EventBuilder.responseFor(event) {}

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(true)
        whenever(eventCacheService.getCachedEvent(userId, eventIdentifier)).thenReturn(responseEvent)

        val expected = responseEvent.copy(
            metadata = JsonObject().apply { addProperty("cached", true) }
        )

        val actual = dispatcher.sendEvent(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
        verifyZeroInteractions(eventDispatcher)
    }

    @Test
    fun `should return from event dispatcher when there is no cached response and cache response`() {
        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)
        val responseEvent = EventBuilder.responseFor(event) {}

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(true)
        whenever(eventCacheService.getCachedEvent(userId, eventIdentifier)).thenReturn(null)
        whenever(eventDispatcher.sendEvent(event)).thenReturn(responseEvent)
        whenever(eventCacheService.shouldCacheEvent(eventIdentifier)).thenReturn(true)

        val expected = responseEvent.copy(
            metadata = JsonObject().apply { addProperty("cached", false) }
        )

        val actual = dispatcher.sendEvent(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
        verify(eventDispatcher).sendEvent(event)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verify(eventCacheService).cacheEvent(userId, eventIdentifier, responseEvent)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should return from event dispatcher when there is no cached response and not cache response`() {
        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)
        val responseEvent = EventBuilder.responseFor(event) {}

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(true)
        whenever(eventCacheService.getCachedEvent(userId, eventIdentifier)).thenReturn(null)
        whenever(eventDispatcher.sendEvent(event)).thenReturn(responseEvent)
        whenever(eventCacheService.shouldCacheEvent(eventIdentifier)).thenReturn(false)

        val expected = responseEvent.copy(
            metadata = JsonObject().apply { addProperty("cached", false) }
        )

        val actual = dispatcher.sendEvent(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
        verify(eventDispatcher).sendEvent(event)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should return from cache on failure`() {
        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)
        val responseEvent = EventBuilder.responseFor(event) {}

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventDispatcher.sendEvent(event)).thenThrow(RuntimeException::class.java)
        whenever(eventCacheService.shouldUseCachedEventOnFailure(eventIdentifier)).thenReturn(true)
        whenever(eventCacheService.getCachedEvent(userId, eventIdentifier)).thenReturn(responseEvent)

        val expected = responseEvent.copy(
            metadata = JsonObject().apply { addProperty("cached", true) }
        )

        val actual = dispatcher.sendEvent(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventDispatcher).sendEvent(event)
        verify(eventCacheService).shouldUseCachedEventOnFailure(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
    }

    @Test
    fun `should throw exception on failure when there is no cached response`() {
        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventDispatcher.sendEvent(event)).thenThrow(RuntimeException::class.java)
        whenever(eventCacheService.shouldUseCachedEventOnFailure(eventIdentifier)).thenReturn(true)
        whenever(eventCacheService.getCachedEvent(userId, eventIdentifier)).thenReturn(null)

        assertThatExceptionOfType(RuntimeException::class.java).isThrownBy { dispatcher.sendEvent(event) }

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventDispatcher).sendEvent(event)
        verify(eventCacheService).shouldUseCachedEventOnFailure(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
    }

    @Test
    fun `should throw exception on failure`() {
        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventDispatcher.sendEvent(event)).thenThrow(RuntimeException::class.java)
        whenever(eventCacheService.shouldUseCachedEventOnFailure(eventIdentifier)).thenReturn(false)

        assertThatExceptionOfType(RuntimeException::class.java).isThrownBy { dispatcher.sendEvent(event) }

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventDispatcher).sendEvent(event)
        verify(eventCacheService).shouldUseCachedEventOnFailure(eventIdentifier)
    }
}
