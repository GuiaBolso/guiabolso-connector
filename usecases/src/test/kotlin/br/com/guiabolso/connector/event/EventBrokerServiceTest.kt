package br.com.guiabolso.connector.event

import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.exception.EventTimeoutException
import br.com.guiabolso.connector.event.exception.FailedDependencyException
import br.com.guiabolso.connector.event.integration.EventBrokerService
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.client.EventClient
import br.com.guiabolso.events.client.adapter.HttpClientAdapter
import br.com.guiabolso.events.client.exception.BadProtocolException
import br.com.guiabolso.events.client.exception.TimeoutException
import br.com.guiabolso.events.json.MapperHolder.mapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventBrokerServiceTest {

    private lateinit var httpClient: HttpClientAdapter
    private lateinit var eventClient: EventClient
    private lateinit var service: EventBrokerService

    @BeforeEach
    fun setUp() {
        httpClient = mock()

        eventClient = EventClient(httpClient)

        service = EventBrokerService(eventClient)
    }

    @Test
    fun `should throw EventException when http client returns ERROR as error code`() {
        val uri = nextObject<String>()
        val timeout = nextObject<Int>()
        val event = buildEvent()
        val rawEvent = mapper.toJson(event)
        val rawResponse = mapper.toJson(EventBuilder.responseFor(event) {
            name = "${event.name}:error"
            payload = mapOf(
                "code" to "ERROR",
                "parameters" to emptyMap<String, String>()
            )
        })

        whenever(httpClient.post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))).thenReturn(rawResponse)

        assertThatExceptionOfType(EventException::class.java).isThrownBy {
            service.sendEvent(uri, event, timeout)
        }.withMessage("ERROR")

        verify(httpClient).post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))
    }

    @Test
    fun `should throw EventTimeoutException when http client returns TIMEOUT as error code`() {
        val uri = nextObject<String>()
        val timeout = nextObject<Int>()
        val event = buildEvent()
        val rawEvent = mapper.toJson(event)

        whenever(httpClient.post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout)))
            .thenThrow(TimeoutException::class.java)

        assertThatExceptionOfType(EventTimeoutException::class.java).isThrownBy {
            service.sendEvent(uri, event, timeout)
        }.withMessage("TIMEOUT")

        verify(httpClient).post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))
    }

    @Test
    fun `should throw FailedDependencyException when http client returns FAILED_DEPENDENCY as error code`() {
        val uri = nextObject<String>()
        val timeout = nextObject<Int>()
        val event = buildEvent()
        val rawEvent = mapper.toJson(event)

        whenever(httpClient.post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout)))
            .thenThrow(BadProtocolException::class.java)

        assertThatExceptionOfType(FailedDependencyException::class.java).isThrownBy {
            service.sendEvent(uri, event, timeout)
        }.withMessage("FAILED_DEPENDENCY")

        verify(httpClient).post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))
    }

    @Test
    fun `should return response event when response type is success`() {
        val uri = nextObject<String>()
        val timeout = nextObject<Int>()
        val event = buildEvent(userId = nextObject<String>())
        val cleanEvent = event.copy(identity = event.identity.deepCopy().apply { remove("userId") })
        val rawEvent = mapper.toJson(cleanEvent)
        val responseEvent = EventBuilder.responseFor(event) {}
        val rawResponse = mapper.toJson(responseEvent)

        whenever(httpClient.post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))).thenReturn(rawResponse)

        val actual = service.sendEvent(uri, event, timeout)

        assertThat(rawEvent.contains("userId", ignoreCase = true)).isFalse()
        assertThat(actual).isEqualTo(responseEvent)
        assertThat(actual.isSuccess()).isTrue()

        verify(httpClient).post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))
    }

    @Test
    fun `should return response event when response type is redirect`() {
        val uri = nextObject<String>()
        val timeout = nextObject<Int>()
        val event = buildEvent(userId = nextObject<String>())
        val cleanEvent = event.copy(identity = event.identity.deepCopy().apply { remove("userId") })
        val rawEvent = mapper.toJson(cleanEvent)
        val responseEvent = EventBuilder.responseFor(event) {
            name = "${event.name}:redirect"
        }
        val rawResponse = mapper.toJson(responseEvent)

        whenever(httpClient.post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))).thenReturn(rawResponse)

        val actual = service.sendEvent(uri, event, timeout)

        assertThat(rawEvent.contains("userId", ignoreCase = true)).isFalse()
        assertThat(actual).isEqualTo(responseEvent)
        assertThat(actual.isRedirect()).isTrue()

        verify(httpClient).post(eq(uri), any(), eq(rawEvent), eq(Charsets.UTF_8), eq(timeout))
    }
}
