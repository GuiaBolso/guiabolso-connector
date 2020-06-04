package br.com.guiabolso.connector.event.model

import java.time.Duration

data class EventCacheConfig(
    val eventName: String,
    val version: EventVersion,
    val duration: Duration,
    val cacheUsagePolicy: CacheUsagePolicy
)
