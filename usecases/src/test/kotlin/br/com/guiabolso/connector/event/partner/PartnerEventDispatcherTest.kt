package br.com.guiabolso.connector.event.partner

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.integration.EventBrokerService
import br.com.guiabolso.connector.event.misc.authenticatedAsClient
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PartnerEventDispatcherTest {

    private lateinit var configService: ConfigService
    private lateinit var eventBrokerService: EventBrokerService
    private lateinit var clientCredentials: ClientCredentials
    private lateinit var dispatcher: PartnerEventDispatcher

    @BeforeEach
    fun setUp() {
        configService = mock()

        whenever(configService.getRequiredString(KASBAH_URL_KEY)).thenReturn(KASBAH_URL_VALUE)
        whenever(configService.getRequiredString(PARTNER_ROUTE_KEY)).thenReturn(PARTNER_ROUTE_VALUE)

        eventBrokerService = mock()

        clientCredentials = ClientCredentials(
            clientId = CLIENT_ID,
            clientSecret = CLIENT_SECRET
        )

        dispatcher = PartnerEventDispatcher(configService, clientCredentials, eventBrokerService)
    }

    @Test
    fun `should return event response`() {
        val uri = KASBAH_URL_VALUE + PARTNER_ROUTE_VALUE
        val event = buildEvent()

        whenever(eventBrokerService.sendEvent(any(), any(), anyOrNull()))
            .thenReturn(EventBuilder.responseFor(event) {})

        val expected = EventBuilder.responseFor(event) {}

        val actual = dispatcher.sendEvent(event)

        assertThat(actual).isEqualTo(expected)

        verify(eventBrokerService).sendEvent(uri, event.authenticatedAsClient(clientCredentials))
    }

    companion object {
        private const val KASBAH_URL_KEY = "kasbah.url"
        private const val KASBAH_URL_VALUE = "kasbah-url"
        private const val PARTNER_ROUTE_KEY = "partner.route"
        private const val PARTNER_ROUTE_VALUE = "/partner"
        private const val CLIENT_ID = "some.client.id"
        private const val CLIENT_SECRET = "some.client.secret"
    }
}
