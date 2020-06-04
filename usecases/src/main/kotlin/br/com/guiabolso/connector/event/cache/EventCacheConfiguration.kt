package br.com.guiabolso.connector.event.cache

import br.com.guiabolso.connector.datapackage.model.DataCache
import br.com.guiabolso.connector.datapackage.model.DataPackageConfiguration
import br.com.guiabolso.connector.event.exception.EventCacheConfigurationException
import br.com.guiabolso.connector.event.model.EventCacheConfig
import br.com.guiabolso.connector.event.model.EventVersion
import br.com.guiabolso.connector.event.model.EventVersion.AllVersions
import br.com.guiabolso.connector.event.model.EventVersion.ExactVersion
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventCacheConfiguration {

    @Bean
    fun eventCacheConfig(configuration: DataPackageConfiguration): List<EventCacheConfig> {
        val dataCacheList = configuration.dataCaches
        return dataCacheList.map { dataCache ->
            EventCacheConfig(
                eventName = dataCache.eventName,
                version = eventVersion(dataCache),
                duration = cacheDuration(dataCache),
                cacheUsagePolicy = dataCache.cacheUsagePolicy
            )
        }
    }

    private fun eventVersion(dataCache: DataCache): EventVersion = when {
        dataCache.eventVersion.trim() == "*" -> AllVersions
        dataCache.eventVersion.toIntOrNull() != null -> ExactVersion(dataCache.eventVersion.toInt())
        else -> throw EventCacheConfigurationException()
    }

    private fun cacheDuration(dataCache: DataCache): Duration {
        val matchResult = DURATION_PATTERN.find(dataCache.duration) ?: throw EventCacheConfigurationException()
        val durationValue = matchResult.groupValues[1].toLong()
        return when (matchResult.groupValues[2]) {
            "day", "days" -> Duration.ofDays(durationValue)
            "hour", "hours", "h" -> Duration.ofHours(durationValue)
            "minute", "minutes", "min" -> Duration.ofMinutes(durationValue)
            "second", "seconds", "s" -> Duration.ofSeconds(durationValue)
            else -> throw EventCacheConfigurationException()
        }
    }

    companion object {
        private val DURATION_PATTERN = "^(\\d+)\\s+(\\w+)$".toRegex()
    }
}
