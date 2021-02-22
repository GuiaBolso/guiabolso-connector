package br.com.guiabolso.connector.common.failure

import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.events.model.EventErrorType
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedirectOnUnauthorizedServiceTest {
    private val redirectUrl = "https://my.url/redirect"
    private lateinit var configService: ConfigService
    private lateinit var redirectOnUnauthorizedService: RedirectOnUnauthorizedService

    @BeforeEach
    fun setUp() {
        configService = mock()
        whenever(configService.getRequiredString("gbconnect.url")).thenReturn(redirectUrl)
    }

    @Test
    fun `should not handle unauthorized exception if policy is NEVER`() {
        whenever(configService.getString(eq("redirect.unauthorized.policy"), any())).thenReturn(RedirectOnUnauthorizedPolicy.NEVER.name)
        redirectOnUnauthorizedService = RedirectOnUnauthorizedService(configService)
        val exception = EventException("fail", emptyMap(), EventErrorType.Unauthorized)

        redirectOnUnauthorizedService.maybeRedirectFor(nextObject(), exception, nextObject())
    }

    @Test
    fun `should do nothing if not unauthorized exception`() {
        whenever(configService.getString(eq("redirect.unauthorized.policy"), any())).thenReturn(RedirectOnUnauthorizedPolicy.NEVER.name)
        redirectOnUnauthorizedService = RedirectOnUnauthorizedService(configService)
        val exception = EventException("fail", emptyMap())

        redirectOnUnauthorizedService.maybeRedirectFor(nextObject(), exception, nextObject())
    }

    @Test
    fun `should handle everything when policy is ALWAYS`() {
        whenever(configService.getString(eq("redirect.unauthorized.policy"), any())).thenReturn(RedirectOnUnauthorizedPolicy.ALWAYS.name)
        redirectOnUnauthorizedService = RedirectOnUnauthorizedService(configService)
        val exception = EventException("fail", emptyMap(), EventErrorType.Unauthorized)

        listOf(RedirectOnUnauthorizedPolicy.USER_EVENTS, RedirectOnUnauthorizedPolicy.PARTNER_EVENTS).forEach {
            assertThatExceptionOfType(RedirectException::class.java)
                .isThrownBy { redirectOnUnauthorizedService.maybeRedirectFor(nextObject(), exception, it) }
                .matches { it.payload.url == redirectUrl }
        }
    }

    @Test
    fun `should handle only one policy`() {
        val policies = listOf(RedirectOnUnauthorizedPolicy.USER_EVENTS, RedirectOnUnauthorizedPolicy.PARTNER_EVENTS).shuffled().toMutableList()
        val randomPolicy = policies.removeAt(0)
        val exception = EventException("fail", emptyMap(), EventErrorType.Unauthorized)
        whenever(configService.getString(eq("redirect.unauthorized.policy"), any())).thenReturn(randomPolicy.name)
        redirectOnUnauthorizedService = RedirectOnUnauthorizedService(configService)

        assertThatExceptionOfType(RedirectException::class.java)
            .isThrownBy { redirectOnUnauthorizedService.maybeRedirectFor(nextObject(), exception, randomPolicy) }
            .matches { it.payload.url == redirectUrl }

        policies.forEach { policy ->
            redirectOnUnauthorizedService.maybeRedirectFor(nextObject(), exception, policy)
        }
    }
}
