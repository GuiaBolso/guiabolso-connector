package br.com.guiabolso.connector.event.user

import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.integration.EventBrokerService
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserEventDispatcherTest {

    private lateinit var configService: ConfigService
    private lateinit var eventBrokerService: EventBrokerService
    private lateinit var dispatcher: UserEventDispatcher

    @BeforeEach
    fun setUp() {
        configService = mock()

        whenever(configService.getRequiredString(KASBAH_URL_KEY)).thenReturn(KASBAH_URL_VALUE)
        whenever(configService.getRequiredString(USER_ROUTE_KEY)).thenReturn(USER_ROUTE_VALUE)

        eventBrokerService = mock()

        dispatcher = UserEventDispatcher(configService, eventBrokerService)
    }

    @Test
    fun `should return event response`() {
        val userId = nextObject<String>()
        val uri = KASBAH_URL_VALUE + USER_ROUTE_VALUE
        val event = buildEvent(userId = userId)

        whenever(eventBrokerService.sendEvent(uri, event))
            .thenReturn(EventBuilder.responseFor(event) {})

        val expected = EventBuilder.responseFor(event) {}

        val actual = dispatcher.sendEvent(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventBrokerService).sendEvent(uri, event)
    }

    companion object {
        private const val KASBAH_URL_KEY = "kasbah.url"
        private const val KASBAH_URL_VALUE = "kasbah-url"
        private const val USER_ROUTE_KEY = "user.route"
        private const val USER_ROUTE_VALUE = "/user"
    }
}
