package br.com.guiabolso.connector.proxy

import br.com.guiabolso.connector.common.auth.AuthenticationService
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.wrapper.LoggingEventHandlerWrapper
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProxyingEventHandlerDiscoveryTest {

    private lateinit var eventDispatcher: EventDispatcher
    private lateinit var discovery: ProxyingEventHandlerDiscovery
    private lateinit var authenticationService: AuthenticationService

    private fun setUp(configService: ConfigService) {
        eventDispatcher = mock()
        authenticationService = mock()

        discovery = ProxyingEventHandlerDiscovery(
            configService,
            eventDispatcher,
            authenticationService
        )
    }

    @Test
    fun `should return event handler with logs`() {
        mock<ConfigService>().also {
            whenever(it.getBoolean(PROPERTY_KEY, PROPERTY_DEFAULT_VALUE)).thenReturn(true)
            setUp(it)
        }

        val eventName = nextObject<String>()
        val eventVersion = nextObject<Int>()

        val actual = discovery.eventHandlerFor(eventName, eventVersion)

        assertThat(actual is LoggingEventHandlerWrapper)
        assertThat(actual?.eventName).isEqualTo(eventName)
        assertThat(actual?.eventVersion).isEqualTo(eventVersion)
    }

    @Test
    fun `should return event handler without logs`() {
        mock<ConfigService>().also {
            whenever(it.getBoolean(PROPERTY_KEY, PROPERTY_DEFAULT_VALUE)).thenReturn(false)
            setUp(it)
        }

        val eventName = nextObject<String>()
        val eventVersion = nextObject<Int>()

        val actual = discovery.eventHandlerFor(eventName, eventVersion)

        assertThat(actual is ProxyingEventHandler)
        assertThat(actual?.eventName).isEqualTo(eventName)
        assertThat(actual?.eventVersion).isEqualTo(eventVersion)
    }

    companion object {
        private const val PROPERTY_KEY = "event.log.startAndFinish"
        private const val PROPERTY_DEFAULT_VALUE = true
    }
}
