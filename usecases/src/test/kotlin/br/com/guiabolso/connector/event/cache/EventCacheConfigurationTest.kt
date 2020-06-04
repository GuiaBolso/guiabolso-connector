package br.com.guiabolso.connector.event.cache

import br.com.guiabolso.connector.datapackage.model.DataCache
import br.com.guiabolso.connector.datapackage.model.DataPackageConfiguration
import br.com.guiabolso.connector.event.exception.EventCacheConfigurationException
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.event.model.CacheUsagePolicy.ALWAYS
import br.com.guiabolso.connector.event.model.EventCacheConfig
import br.com.guiabolso.connector.event.model.EventVersion.AllVersions
import br.com.guiabolso.connector.event.model.EventVersion.ExactVersion
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventCacheConfigurationTest {

    private lateinit var configuration: EventCacheConfiguration

    @BeforeEach
    fun setUp() {
        configuration = EventCacheConfiguration()
    }

    @Test
    fun `should throw exception when event version is invalid`() {
        val dataPackageConfiguration = DataPackageConfiguration(
            dataPackages = emptyList(),
            dataCaches = listOf(
                nextObject<DataCache>().copy(eventVersion = "?")
            )
        )

        assertThatExceptionOfType(EventCacheConfigurationException::class.java).isThrownBy {
            configuration.eventCacheConfig(dataPackageConfiguration)
        }
    }

    @Test
    fun `should throw exception when duration not match pattern`() {
        val dataPackageConfiguration = DataPackageConfiguration(
            dataPackages = emptyList(),
            dataCaches = listOf(
                DataCache(
                    eventName = nextObject(),
                    eventVersion = nextObject<Int>().toString(),
                    duration = "?",
                    cacheUsagePolicy = nextObject()
                )
            )
        )

        assertThatExceptionOfType(EventCacheConfigurationException::class.java).isThrownBy {
            configuration.eventCacheConfig(dataPackageConfiguration)
        }
    }

    @Test
    fun `should throw exception when duration is invalid`() {
        val dataPackageConfiguration = DataPackageConfiguration(
            dataPackages = emptyList(),
            dataCaches = listOf(
                DataCache(
                    eventName = nextObject(),
                    eventVersion = nextObject<Int>().toString(),
                    duration = "1 year",
                    cacheUsagePolicy = nextObject()
                )
            )
        )

        assertThatExceptionOfType(EventCacheConfigurationException::class.java).isThrownBy {
            configuration.eventCacheConfig(dataPackageConfiguration)
        }
    }

    @Test
    fun `should return a list of EventCacheConfig`() {
        val dataPackageConfiguration = DataPackageConfiguration(
            dataPackages = emptyList(),
            dataCaches = listOf(
                DataCache(
                    eventName = "some:event1",
                    eventVersion = "*",
                    duration = "1 day",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event2",
                    eventVersion = "1",
                    duration = "2 days",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event3",
                    eventVersion = "1",
                    duration = "1 hour",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event4",
                    eventVersion = "1",
                    duration = "2 hours",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event5",
                    eventVersion = "1",
                    duration = "3 h",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event6",
                    eventVersion = "1",
                    duration = "1 minute",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event7",
                    eventVersion = "1",
                    duration = "2 minutes",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event8",
                    eventVersion = "1",
                    duration = "3 min",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event9",
                    eventVersion = "1",
                    duration = "1 second",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event10",
                    eventVersion = "1",
                    duration = "2 seconds",
                    cacheUsagePolicy = ALWAYS
                ), DataCache(
                    eventName = "some:event11",
                    eventVersion = "1",
                    duration = "3 s",
                    cacheUsagePolicy = ALWAYS
                )
            )
        )

        val expected = listOf(
            EventCacheConfig(
                eventName = "some:event1",
                version = AllVersions,
                duration = Duration.ofDays(1),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event2",
                version = ExactVersion(1),
                duration = Duration.ofDays(2),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event3",
                version = ExactVersion(1),
                duration = Duration.ofHours(1),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event4",
                version = ExactVersion(1),
                duration = Duration.ofHours(2),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event5",
                version = ExactVersion(1),
                duration = Duration.ofHours(3),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event6",
                version = ExactVersion(1),
                duration = Duration.ofMinutes(1),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event7",
                version = ExactVersion(1),
                duration = Duration.ofMinutes(2),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event8",
                version = ExactVersion(1),
                duration = Duration.ofMinutes(3),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event9",
                version = ExactVersion(1),
                duration = Duration.ofSeconds(1),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event10",
                version = ExactVersion(1),
                duration = Duration.ofSeconds(2),
                cacheUsagePolicy = ALWAYS
            ), EventCacheConfig(
                eventName = "some:event11",
                version = ExactVersion(1),
                duration = Duration.ofSeconds(3),
                cacheUsagePolicy = ALWAYS
            )
        )

        val actual = configuration.eventCacheConfig(dataPackageConfiguration)

        assertThat(actual).isEqualTo(expected)
    }
}
