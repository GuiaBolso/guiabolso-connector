package br.com.guiabolso.connector.handlers.oauth

import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.misc.buildEvent
import br.com.guiabolso.connector.token.TokenExchangeService
import br.com.guiabolso.events.builder.EventBuilder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthorizationCodeEventHandlerTest {

    private lateinit var tokenExchangeService: TokenExchangeService
    private lateinit var handler: AuthorizationCodeEventHandler

    @BeforeEach
    fun setUp() {
        tokenExchangeService = mock()
        handler = AuthorizationCodeEventHandler(tokenExchangeService)
    }

    @Test
    fun `should handle event`() {
        val userId = nextObject<String>()
        val authorizationCode = nextObject<String>()

        val event = buildEvent(
            name = EVENT_NAME,
            version = EVENT_VERSION,
            userId = userId,
            payload = mapOf("authorizationCode" to authorizationCode)
        )

        val responseEvent = EventBuilder.responseFor(event) {
            payload = emptyMap<String, String>()
        }

        val actual = handler.handle(event)

        assertThat(actual).isEqualTo(responseEvent)

        verify(tokenExchangeService).exchangeTokens(userId, authorizationCode)
    }

    companion object {
        private const val EVENT_NAME = "authorization:code"
        private const val EVENT_VERSION = 1
    }
}
