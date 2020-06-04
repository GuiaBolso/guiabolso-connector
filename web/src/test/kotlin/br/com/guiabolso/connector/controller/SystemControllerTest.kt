package br.com.guiabolso.connector.controller

import br.com.guiabolso.connector.WebTestCase
import br.com.guiabolso.connector.WebTestConfiguration.Companion.PORT
import io.restassured.RestAssured.given
import io.restassured.http.ContentType.JSON
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.Test

class SystemControllerTest : WebTestCase() {

    @Test
    fun `should return status 200 for index`() {
        given()
            .port(PORT)
            .`when`()
            .get("/")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("status", `is`(true))
    }

    @Test
    fun `should return status 200 for health`() {
        given()
            .port(PORT)
            .`when`()
            .get("/health")
            .then()
            .statusCode(200)
            .contentType(JSON)
            .body("status", `is`(true))
    }
}
