package br.com.guiabolso.connector.event.partner

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedPolicy
import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedService
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.connector.event.integration.EventBrokerService
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper
import br.com.guiabolso.connector.event.misc.authenticatedAsClient
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.model.EventErrorType
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PartnerEventDispatcherTest {

    private lateinit var configService: ConfigService
    private lateinit var eventBrokerService: EventBrokerService
    private lateinit var clientCredentials: ClientCredentials
    private lateinit var redirectOnUnauthorizedService: RedirectOnUnauthorizedService
    private lateinit var dispatcher: PartnerEventDispatcher

    @BeforeEach
    fun setUp() {
        configService = mock()

        whenever(configService.getRequiredString(KASBAH_URL_KEY)).thenReturn(KASBAH_URL_VALUE)
        whenever(configService.getRequiredString(PARTNER_ROUTE_KEY)).thenReturn(PARTNER_ROUTE_VALUE)

        eventBrokerService = mock()
        redirectOnUnauthorizedService = mock()

        clientCredentials = ClientCredentials(
            clientId = CLIENT_ID,
            clientSecret = CLIENT_SECRET
        )

        dispatcher = PartnerEventDispatcher(configService, clientCredentials, eventBrokerService, redirectOnUnauthorizedService)
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

    @Test
    fun `should redirect if unauthorized exception and PARTNER_EVENTS policy active`() {
        val uri = KASBAH_URL_VALUE + PARTNER_ROUTE_VALUE
        val event = buildEvent()
        val ex = EventException("failed", emptyMap(), EventErrorType.Unauthorized)
        val redirect = RedirectException(event, EasyRandomWrapper.nextObject(), ex)

        whenever(eventBrokerService.sendEvent(any(), any(), anyOrNull())).thenThrow(ex)
        whenever(redirectOnUnauthorizedService.maybeRedirectFor(any(), eq(ex), eq(RedirectOnUnauthorizedPolicy.PARTNER_EVENTS)))
            .thenThrow(redirect)

        Assertions.assertThatExceptionOfType(RedirectException::class.java).isThrownBy { dispatcher.sendEvent(event) }

        verify(eventBrokerService).sendEvent(uri, event.authenticatedAsClient(clientCredentials))
        verify(redirectOnUnauthorizedService)
            .maybeRedirectFor(event, ex, RedirectOnUnauthorizedPolicy.PARTNER_EVENTS)
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
