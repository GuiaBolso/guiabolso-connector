package br.com.guiabolso.connector.auth

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.connector.event.misc.requiredString
import br.com.guiabolso.connector.token.AccessTokenProvider
import br.com.guiabolso.connector.token.exception.ExpiredRefreshTokenException
import br.com.guiabolso.events.builder.EventBuilder
import com.google.gson.JsonObject
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EventAuthenticationServiceTest {

    private lateinit var accessTokenProvider: AccessTokenProvider
    private lateinit var clientCredentials: ClientCredentials
    private lateinit var configService: ConfigService
    private lateinit var eventAuthenticationService: EventAuthenticationService

    @BeforeEach
    fun setUp() {
        accessTokenProvider = mock()
        configService = mock()
        clientCredentials = ClientCredentials(
            clientId = UUID.randomUUID().toString(),
            clientSecret = UUID.randomUUID().toString()
        )
        whenever(configService.getRequiredString(any())).thenReturn("https://connect.test")
        eventAuthenticationService = EventAuthenticationService(accessTokenProvider, clientCredentials, configService)
    }

    @Test
    fun `can authenticate event`() {
        val req = EventBuilder.event {
            name = "event:test"
            version = 1
            id = UUID.randomUUID().toString()
            flowId = UUID.randomUUID().toString()
            payload = JsonObject()
            auth = JsonObject()
            identity = JsonObject().apply { addProperty("userId", "test") }
            metadata = JsonObject()
        }
        whenever(accessTokenProvider.getAccessTokenBy(any())).thenReturn("some-access-token")

        val result = eventAuthenticationService.authenticate(req)

        Assertions.assertEquals(clientCredentials.clientSecret, result.auth.requiredString("clientSecret"))
        Assertions.assertEquals("some-access-token", result.auth.requiredString("accessToken"))
        Assertions.assertEquals(clientCredentials.clientId, result.identity.requiredString("clientId"))
    }

    @Test
    fun `cannot authenticate event because of ExpiredRefreshTokenException`() {
        val req = EventBuilder.event {
            name = "event:test"
            version = 1
            id = UUID.randomUUID().toString()
            flowId = UUID.randomUUID().toString()
            payload = JsonObject()
            auth = JsonObject()
            identity = JsonObject().apply { addProperty("userId", "test") }
            metadata = JsonObject()
        }
        whenever(accessTokenProvider.getAccessTokenBy(any())).thenThrow(ExpiredRefreshTokenException::class.java)

        assertThrows(RedirectException::class.java) {
            eventAuthenticationService.authenticate(req)
        }
    }
}
