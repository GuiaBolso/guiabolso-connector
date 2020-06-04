package br.com.guiabolso.connector.common.time

import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UTCDateTimeServiceTest {

    private lateinit var service: UTCDateTimeService

    @BeforeEach
    fun setUp() {
        service = UTCDateTimeService()
    }

    @Test
    fun `should return now in ZonedDateTime`() {
        val actual = service.now()

        val expected = ZonedDateTime.now(ZoneOffset.UTC)

        assertThat(actual).isCloseTo(expected, within(1, ChronoUnit.SECONDS))
    }
}
