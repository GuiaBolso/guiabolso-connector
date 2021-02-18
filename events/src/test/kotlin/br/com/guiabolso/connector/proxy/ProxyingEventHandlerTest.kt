package br.com.guiabolso.connector.proxy

import br.com.guiabolso.connector.common.auth.AuthenticationService
import br.com.guiabolso.connector.common.auth.NoOpAuthenticationService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProxyingEventHandlerTest {

    private lateinit var eventName: String
    private var eventVersion: Int = 0
    private lateinit var dispatcher: EventDispatcher
    private lateinit var handler: ProxyingEventHandler
    private lateinit var authenticationService: AuthenticationService

    @BeforeEach
    fun setUp() {
        eventName = nextObject()
        eventVersion = nextObject()
        dispatcher = mock()
        authenticationService = NoOpAuthenticationService

        handler = ProxyingEventHandler(
            eventName,
            eventVersion,
            dispatcher,
            authenticationService
        )
    }

    @Test
    fun `should proxy request event to dispatcher`() {
        val event = buildEvent()
        val responseEvent = EventBuilder.responseFor(event) {}

        whenever(dispatcher.sendEvent(event)).thenReturn(responseEvent)

        val actual = handler.handle(event)

        assertThat(actual).isEqualTo(responseEvent)

        verify(dispatcher).sendEvent(event)
    }
}
