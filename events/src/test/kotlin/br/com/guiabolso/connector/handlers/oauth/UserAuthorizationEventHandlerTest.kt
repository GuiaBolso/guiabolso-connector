package br.com.guiabolso.connector.handlers.oauth

import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.misc.buildEvent
import br.com.guiabolso.connector.user.UserAuthorizationService
import br.com.guiabolso.events.builder.EventBuilder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserAuthorizationEventHandlerTest {

    private lateinit var userAuthorizationService: UserAuthorizationService
    private lateinit var handler: UserAuthorizationEventHandler

    @BeforeEach
    fun setUp() {
        userAuthorizationService = mock()
        handler = UserAuthorizationEventHandler(userAuthorizationService)
    }

    @Test
    fun `should handle event`() {
        val userId = nextObject<String>()
        val event = buildEvent(
            name = EVENT_NAME,
            version = EVENT_VERSION,
            userId = userId
        )

        val responseEvent = EventBuilder.responseFor(event) {
            payload = true
        }

        whenever(userAuthorizationService.isUserAuthorized(userId)).thenReturn(true)

        val actual = handler.handle(event)

        assertThat(actual).isEqualTo(responseEvent)
    }

    companion object {
        private const val EVENT_NAME = "is:user:authorized"
        private const val EVENT_VERSION = 1
    }
}
