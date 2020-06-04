package br.com.guiabolso.connector.controller

import br.com.guiabolso.connector.WebTestCase
import br.com.guiabolso.connector.WebTestConfiguration.Companion.PORT
import br.com.guiabolso.events.json.MapperHolder.mapper
import br.com.guiabolso.events.server.EventProcessor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.restassured.RestAssured.given
import org.hamcrest.CoreMatchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class EventControllerTest : WebTestCase() {

    @Autowired
    @Qualifier("partnerEventProcessor")
    private lateinit var partnerEventProcessor: EventProcessor

    @Autowired
    @Qualifier("userEventProcessor")
    private lateinit var userEventProcessor: EventProcessor

    @Test
    fun `should return status 200 for partner event`() {
        val body = mapper.toJson(mapOf("event" to "some:event"))
        val response = mapper.toJson(mapOf("event" to "some:event:response"))

        whenever(partnerEventProcessor.processEvent(body)).thenReturn(response)

        given()
            .port(PORT)
            .body(body)
            .`when`()
            .post("/partner/events/")
            .then()
            .statusCode(200)
            .body("event", equalTo("some:event:response"))

        verify(partnerEventProcessor).processEvent(body)
        verifyZeroInteractions(userEventProcessor)
    }

    @Test
    fun `should return status 200 for user event`() {
        val body = mapper.toJson(mapOf("event" to "some:event"))
        val response = mapper.toJson(mapOf("event" to "some:event:response"))

        whenever(userEventProcessor.processEvent(body)).thenReturn(response)

        given()
            .port(PORT)
            .body(body)
            .`when`()
            .post("/gbConnect/events/")
            .then()
            .statusCode(200)
            .body("event", equalTo("some:event:response"))

        verify(userEventProcessor).processEvent(body)
        verifyZeroInteractions(partnerEventProcessor)
    }
}
