package br.com.guiabolso.connector.datapackage

import br.com.guiabolso.connector.common.auth.AuthenticationService
import br.com.guiabolso.connector.common.auth.NoOpAuthenticationService
import br.com.guiabolso.connector.datapackage.model.DataPackage
import br.com.guiabolso.connector.datapackage.model.PackageSource
import br.com.guiabolso.connector.datapackage.model.Uts
import br.com.guiabolso.connector.datapackage.model.UtsVariable
import br.com.guiabolso.connector.event.cache.CachedEventDispatcher
import br.com.guiabolso.connector.event.cache.EventCacheService
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.connector.event.model.EventIdentifier
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
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class DataPackageServiceTest {

    private lateinit var eventDispatcher: CachedEventDispatcher
    private lateinit var eventCacheService: EventCacheService
    private lateinit var authenticationService: AuthenticationService
    private lateinit var service: DataPackageService

    private fun setUp() {
        EventContextHolder.setContext(
            context = EventContext(
                id = nextObject(),
                flowId = nextObject()
            )
        )

        eventDispatcher = mock()
        eventCacheService = mock()
        authenticationService = NoOpAuthenticationService

        service = DataPackageService(eventCacheService, eventDispatcher, authenticationService)
    }

    @Test
    fun `should not handle event when there is no source`() {
        setUp()

        val dataPackage = DataPackage(
            publish = nextObject(),
            sources = emptyList()
        )
        val event = buildEvent(userId = nextObject())
        val eventIdentifier = EventIdentifier(event.name, event.version)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventCacheService.shouldCacheEvent(eventIdentifier)).thenReturn(false)

        val expected = EventBuilder.responseFor(event) {
            payload = mapOf<String, Any>("variables" to emptyList<Any>())
            metadata = mapOf<String, Any>("cached" to false)
        }

        val actual = service.handleDataPackage(dataPackage, event)

        Assertions.assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verifyZeroInteractions(eventDispatcher)
        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should ignore duplicated variable`() {
        setUp()

        val source1 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.1")
        val source2 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.2")
        val dataPackage = DataPackage(
            publish = nextObject(),
            sources = listOf(source1, source2)
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

        val actual = service.handleDataPackage(dataPackage, event)
        val actualVariables = actual.payloadAs<Uts>().variables!!.sortedBy { it.key }

        Assertions.assertThat(actualVariables).isEqualTo(expected)

        verify(eventDispatcher).sendEvent(sourceEvent1)
        verify(eventDispatcher).sendEvent(sourceEvent2)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should handle events and merge responses`() {
        setUp()

        val source1 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.1")
        val source2 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.2")
        val dataPackage = DataPackage(
            publish = nextObject(),
            sources = listOf(source1, source2)
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

        val actual = service.handleDataPackage(dataPackage, event)
        val actualVariables = actual.payloadAs<Uts>().variables!!.sortedBy { it.key }

        Assertions.assertThat(actualVariables).isEqualTo(expected)

        verify(eventDispatcher).sendEvent(sourceEvent1)
        verify(eventDispatcher).sendEvent(sourceEvent2)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)
        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should return cached event on error`() {
        setUp()

        val source = nextObject<PackageSource>()
        val dataPackage = DataPackage(
            publish = nextObject(),
            sources = listOf(source)
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

        val actual = service.handleDataPackage(dataPackage, event)

        Assertions.assertThat(actual).isEqualTo(expected)

        verify(eventCacheService).shouldUseCachedEventOnFailure(eventIdentifier)
        verify(eventCacheService).getCachedEvent(userId, eventIdentifier)
        verify(eventDispatcher).sendEvent(sourceEvent)
        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
    }

    @Test
    fun `should cache event`() {
        setUp()

        val source1 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.1")
        val source2 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.2")
        val dataPackage = DataPackage(
            publish = nextObject(),
            sources = listOf(source1, source2)
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

        val actual = service.handleDataPackage(dataPackage, event)
        val actualVariables = actual.payloadAs<Uts>().variables!!.sortedBy { it.key }

        Assertions.assertThat(actualVariables).hasSameElementsAs(expected)

        verify(eventDispatcher).sendEvent(sourceEvent1)
        verify(eventDispatcher).sendEvent(sourceEvent2)
        verify(eventCacheService).shouldCacheEvent(eventIdentifier)

        argumentCaptor<ResponseEvent>().run {
            verify(eventCacheService).cacheEvent(eq(userId), eq(eventIdentifier), capture())

            Assertions.assertThat(firstValue).isEqualToIgnoringGivenFields(response, "payload")
            Assertions.assertThat(firstValue.payloadAs(Uts::class.java).variables).hasSameElementsAs(expected)
        }
    }

    @Test
    fun `should throws when exception is Redirect`() {
        setUp()

        val source = nextObject<PackageSource>()
        val dataPackage = DataPackage(
            publish = nextObject(),
            sources = listOf(source)
        )
        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)
        val eventIdentifier = EventIdentifier(event.name, event.version)

        val sourceEvent = buildEvent(name = source.eventName, version = source.eventVersion, userId = userId)

        whenever(eventCacheService.shouldUseCachedEvent(eventIdentifier)).thenReturn(false)
        whenever(eventDispatcher.sendEvent(sourceEvent)).thenThrow(RedirectException::class.java)

        assertThatExceptionOfType(RedirectException::class.java)
            .isThrownBy { service.handleDataPackage(dataPackage, event) }

        verify(eventCacheService).shouldUseCachedEvent(eventIdentifier)
        verifyNoMoreInteractions(eventCacheService)
        verify(eventDispatcher).sendEvent(sourceEvent)
    }
}
