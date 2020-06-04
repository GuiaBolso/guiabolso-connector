package br.com.guiabolso.connector.handlers

import br.com.guiabolso.connector.auth.AuthenticationService
import br.com.guiabolso.connector.auth.NoOpAuthenticationService
import br.com.guiabolso.connector.datapackage.model.DataPackage
import br.com.guiabolso.connector.datapackage.model.PackageSource
import br.com.guiabolso.connector.event.cache.CachedEventDispatcher
import br.com.guiabolso.connector.event.cache.EventCacheService
import br.com.guiabolso.connector.event.model.EventIdentifier
import br.com.guiabolso.connector.handlers.models.Uts
import br.com.guiabolso.connector.handlers.models.UtsVariable
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.context.EventContext
import br.com.guiabolso.events.context.EventContextHolder
import br.com.guiabolso.events.model.ResponseEvent
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MergingEventHandlerTest {

    private lateinit var eventDispatcher: CachedEventDispatcher
    private lateinit var eventCacheService: EventCacheService
    private lateinit var authenticationService: AuthenticationService
    private lateinit var handler: MergingEventHandler

    private fun setUp(dataPackage: DataPackage) {
        EventContextHolder.setContext(
            context = EventContext(
                id = nextObject<String>(),
                flowId = nextObject<String>()
            )
        )

        eventDispatcher = mock()
        authenticationService = NoOpAuthenticationService
        eventCacheService = mock()

        handler = MergingEventHandler(dataPackage, eventDispatcher, authenticationService, eventCacheService)
    }

    @Test
    fun `should not handle event when there is no source`() {
        setUp(
            dataPackage = DataPackage(
                publish = nextObject(),
                sources = emptyList()
            )
        )

        val event = buildEvent(userId = nextObject())
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventCacheService.shouldCacheEvent(eventIdentifier)).thenReturn(false)

        val expected = EventBuilder.responseFor(event) {
            payload = mapOf<String, Any>("variables" to emptyList<Any>())
            metadata = mapOf<String, Any>("cached" to false)
        }

        val actual = handler.handle(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should ignore duplicated variable`() {
        val source1 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.1")
        val source2 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.2")

        setUp(
            DataPackage(
                publish = nextObject(),
                sources = listOf(source1, source2)
            )
        )

        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventCacheService.shouldCacheEvent(eventIdentifier)).thenReturn(false)

        val sourceEvent1 = buildEvent(name = source1.eventName, version = source1.eventVersion, userId = userId)
        val responseSourceEvent1 = Uts(listOf(UtsVariable(key = "TEST.VAR.V1", value = 42, type = "INT32")))
        whenever(eventDispatcher.sendEvent(sourceEvent1)).thenReturn(EventBuilder.responseFor(sourceEvent1) {
            payload = responseSourceEvent1
        })

        val sourceEvent2 = buildEvent(name = source2.eventName, version = source2.eventVersion, userId = userId)
        val responseSourceEvent2 = Uts(listOf(UtsVariable(key = "TEST.VAR.V1", value = 42, type = "INT32")))
        whenever(eventDispatcher.sendEvent(sourceEvent2)).thenReturn(EventBuilder.responseFor(sourceEvent2) {
            payload = responseSourceEvent2
        })

        val expected = listOf(
            responseSourceEvent1.variables!!.first(),
            UtsVariable(key = source1.statusKey, value = "SUCCESS", type = "STRING"),
            UtsVariable(key = source2.statusKey, value = "SUCCESS", type = "STRING")
        ).sortedBy { it.key }

        val actual = handler.handle(event)
        val actualVariables = actual.payloadAs<Uts>().variables!!.sortedBy { it.key }

        assertThat(actualVariables).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventDispatcher).sendEvent(sourceEvent1)
        verify(eventDispatcher).sendEvent(sourceEvent2)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should handle events and merge responses`() {
        val source1 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.1")
        val source2 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.2")

        setUp(
            DataPackage(
                publish = nextObject(),
                sources = listOf(source1, source2)
            )
        )

        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventCacheService.shouldCacheEvent(eventIdentifier)).thenReturn(false)

        val sourceEvent1 = buildEvent(name = source1.eventName, version = source1.eventVersion, userId = userId)
        val responseSourceEvent1 = Uts(listOf(UtsVariable(key = "TEST.VAR.V1", value = 42, type = "INT32")))
        whenever(eventDispatcher.sendEvent(sourceEvent1)).thenReturn(EventBuilder.responseFor(sourceEvent1) {
            payload = responseSourceEvent1
        })

        val sourceEvent2 = buildEvent(name = source2.eventName, version = source2.eventVersion, userId = userId)
        val responseSourceEvent2 = Uts(listOf(UtsVariable(key = "TEST.VAR.V2", value = "xpto", type = "STRING")))
        whenever(eventDispatcher.sendEvent(sourceEvent2)).thenReturn(EventBuilder.responseFor(sourceEvent2) {
            payload = responseSourceEvent2
        })

        val expected = listOf(
            responseSourceEvent1.variables!!.first(),
            responseSourceEvent2.variables!!.first(),
            UtsVariable(key = source1.statusKey, value = "SUCCESS", type = "STRING"),
            UtsVariable(key = source2.statusKey, value = "SUCCESS", type = "STRING")
        ).sortedBy { it.key }

        val actual = handler.handle(event)
        val actualVariables = actual.payloadAs<Uts>().variables!!.sortedBy { it.key }

        assertThat(actualVariables).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventDispatcher).sendEvent(sourceEvent1)
        verify(eventDispatcher).sendEvent(sourceEvent2)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should return cached event`() {
        val source = nextObject<PackageSource>()

        setUp(
            DataPackage(
                publish = nextObject(),
                sources = listOf(source)
            )
        )

        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(true)

        val response = EventBuilder.responseFor(event) {}
        whenever(eventCacheService.getCachedEvent(userId, eventIdentifier)).thenReturn(response)

        val expected = EventBuilder.responseFor(event) {
            metadata = mapOf<String, Any>("cached" to true)
        }

        val actual = handler.handle(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
        verifyZeroInteractions(eventDispatcher)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should return cached event on error`() {
        val source = nextObject<PackageSource>()

        setUp(
            DataPackage(
                publish = nextObject(),
                sources = listOf(source)
            )
        )

        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        val sourceEvent = buildEvent(name = source.eventName, version = source.eventVersion, userId = userId)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventDispatcher.sendEvent(sourceEvent)).thenThrow(RuntimeException::class.java)
        whenever(eventCacheService.shouldUseCachedEventOnFailure(eventIdentifier)).thenReturn(true)

        val response = EventBuilder.responseFor(sourceEvent) {}
        whenever(eventCacheService.getCachedEvent(userId, eventIdentifier)).thenReturn(response)

        val expected = EventBuilder.responseFor(event) {
            metadata = mapOf<String, Any>("cached" to true)
        }

        val actual = handler.handle(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventCacheService).shouldUseCachedEventOnFailure(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
        verify(eventDispatcher).sendEvent(sourceEvent)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should cache event`() {
        val source1 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.1")
        val source2 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.2")

        setUp(
            DataPackage(
                publish = nextObject(),
                sources = listOf(source1, source2)
            )
        )

        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)

        val sourceEvent1 = buildEvent(name = source1.eventName, version = source1.eventVersion, userId = userId)
        val responseSourceEvent1 = Uts(listOf(UtsVariable(key = "TEST.VAR.V1", value = "qwerty", type = "STRING")))
        whenever(eventDispatcher.sendEvent(sourceEvent1)).thenReturn(EventBuilder.responseFor(sourceEvent1) {
            payload = responseSourceEvent1
        })

        val sourceEvent2 = buildEvent(name = source2.eventName, version = source2.eventVersion, userId = userId)
        val responseSourceEvent2 = Uts(listOf(UtsVariable(key = "TEST.VAR.V2", value = "xpto", type = "STRING")))
        whenever(eventDispatcher.sendEvent(sourceEvent2)).thenReturn(EventBuilder.responseFor(sourceEvent2) {
            payload = responseSourceEvent2
        })

        whenever(eventCacheService.shouldCacheEvent(eventIdentifier)).thenReturn(true)

        val expected = listOf(
            responseSourceEvent2.variables!!.first(),
            responseSourceEvent1.variables!!.first(),
            UtsVariable(key = source1.statusKey, value = "SUCCESS", type = "STRING"),
            UtsVariable(key = source2.statusKey, value = "SUCCESS", type = "STRING")
        )

        val response = EventBuilder.responseFor(event) {
            payload = Uts(expected)
        }

        val actual = handler.handle(event)
        val actualVariables = actual.payloadAs<Uts>().variables!!.sortedBy { it.key }

        assertThat(actualVariables).hasSameElementsAs(expected)

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verify(eventDispatcher).sendEvent(sourceEvent1)
        verify(eventDispatcher).sendEvent(sourceEvent2)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)

        argumentCaptor<ResponseEvent>().run {
            verify(eventCacheService).cacheEvent(eq(userId), eq(eventIdentifier), capture())

            assertThat(firstValue).isEqualToIgnoringGivenFields(response, "payload")
            assertThat(firstValue.payloadAs(Uts::class.java).variables).hasSameElementsAs(expected)
        }
    }
}
