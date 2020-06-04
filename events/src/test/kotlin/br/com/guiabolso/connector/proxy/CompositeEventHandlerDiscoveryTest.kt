package br.com.guiabolso.connector.proxy

import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.events.server.handler.EventHandler
import br.com.guiabolso.events.server.handler.EventHandlerDiscovery
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CompositeEventHandlerDiscoveryTest {

    private lateinit var eventHandlerDiscovery1: EventHandlerDiscovery
    private lateinit var eventHandlerDiscovery2: EventHandlerDiscovery
    private lateinit var discovery: CompositeEventHandlerDiscovery

    @BeforeEach
    fun setUp() {
        eventHandlerDiscovery1 = mock()
        eventHandlerDiscovery2 = mock()

        discovery = CompositeEventHandlerDiscovery(
            listOf(
                eventHandlerDiscovery1,
                eventHandlerDiscovery2
            )
        )
    }

    @Test
    fun `should return null when there is no event handler discoveries`() {
        val emptyDiscovery = CompositeEventHandlerDiscovery(emptyList())

        val eventName = nextObject<String>()
        val eventVersion = nextObject<Int>()

        val actual = emptyDiscovery.eventHandlerFor(eventName, eventVersion)

        assertThat(actual).isNull()
    }

    @Test
    fun `should return null when there is no match`() {
        val eventName = nextObject<String>()
        val eventVersion = nextObject<Int>()

        whenever(eventHandlerDiscovery1.eventHandlerFor(eventName, eventVersion)).thenReturn(null)
        whenever(eventHandlerDiscovery2.eventHandlerFor(eventName, eventVersion)).thenReturn(null)

        val actual = discovery.eventHandlerFor(eventName, eventVersion)

        assertThat(actual).isNull()

        verify(eventHandlerDiscovery1).eventHandlerFor(eventName, eventVersion)
        verify(eventHandlerDiscovery2).eventHandlerFor(eventName, eventVersion)
    }

    @Test
    fun `should return event handler`() {
        val eventName = nextObject<String>()
        val eventVersion = nextObject<Int>()
        val eventHandler = nextObject<EventHandler>()

        whenever(eventHandlerDiscovery1.eventHandlerFor(eventName, eventVersion)).thenReturn(eventHandler)

        val actual = discovery.eventHandlerFor(eventName, eventVersion)

        assertThat(actual).isEqualTo(eventHandler)

        verify(eventHandlerDiscovery1).eventHandlerFor(eventName, eventVersion)
    }
}
