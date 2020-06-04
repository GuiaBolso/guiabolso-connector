package br.com.guiabolso.connector.wrapper

import br.com.guiabolso.connector.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.server.handler.EventHandler
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoggingEventHandlerWrapperTest {

    private lateinit var eventHandler: EventHandler
    private lateinit var wrapper: LoggingEventHandlerWrapper

    @BeforeEach
    fun setUp() {
        eventHandler = mock()
        wrapper = LoggingEventHandlerWrapper(eventHandler)
    }

    @Test
    fun `should handle event`() {
        val event = buildEvent()
        val responseEvent = EventBuilder.responseFor(event) {}

        whenever(eventHandler.handle(event)).thenReturn(responseEvent)

        val actual = wrapper.handle(event)

        assertThat(actual).isEqualTo(responseEvent)
    }
}
