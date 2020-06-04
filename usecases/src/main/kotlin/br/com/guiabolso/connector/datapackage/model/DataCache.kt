package br.com.guiabolso.connector.datapackage.model

import br.com.guiabolso.connector.event.model.CacheUsagePolicy

data class DataCache(
    val eventName: String,
    val eventVersion: String,
    val duration: String,
    val cacheUsagePolicy: CacheUsagePolicy
)
