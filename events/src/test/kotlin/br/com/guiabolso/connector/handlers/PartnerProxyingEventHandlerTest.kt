package br.com.guiabolso.connector.handlers

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.server.handler.EventHandler
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PartnerProxyingEventHandlerTest {

    private lateinit var eventHandler: EventHandler
    private lateinit var clientCredentials: ClientCredentials

    @BeforeEach
    private fun setUp() {
        eventHandler = mock()
        clientCredentials = mock()
    }

    @Test
    fun `should send to delegate event handler containing its event name and version`() {
        whenever(eventHandler.eventName).thenReturn("event:name")
        whenever(eventHandler.eventVersion).thenReturn(1)

        whenever(clientCredentials.clientId).thenReturn("client.id")
        whenever(clientCredentials.clientSecret).thenReturn("client.secret")

        val partnerProxyingEventHandler = PartnerProxyingEventHandler(eventHandler, clientCredentials)

        assertThat(partnerProxyingEventHandler.eventName).isEqualTo("event:name")
        assertThat(partnerProxyingEventHandler.eventVersion).isEqualTo(1)

        partnerProxyingEventHandler.handle(nextObject<RequestEvent>())

        argumentCaptor<RequestEvent>().run {
            verify(eventHandler).handle(capture())

            assertThat(firstValue.identity.asJsonObject.get("clientId").asString).isEqualTo("client.id")
            assertThat(firstValue.auth.asJsonObject.get("clientSecret").asString).isEqualTo("client.secret")
        }
    }
}
