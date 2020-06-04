package br.com.guiabolso.connector.common.cache

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class CacheService(
    caches: List<MultiLevelDistributedCache>
) {

    private val cache: CacheNode?

    init {
        if (caches.isEmpty()) logger.warn("No distributed cache implementation found.")
        cache = caches
            .sortedWith(
                compareBy<MultiLevelDistributedCache> { it.isInMemory() }
                    .thenByDescending { it.level }
            )
            .fold<DistributedCache, CacheNode?>(null) { acc, cache ->
                CacheNode(cache, acc)
            }
    }

    fun getData(key: String, duration: Duration, onlyInMemory: Boolean): EncryptedData? {
        return cache?.getData(key, duration, onlyInMemory)
    }

    fun putData(key: String, value: EncryptedData, duration: Duration, onlyInMemory: Boolean) {
        cache?.putData(key, value, duration, onlyInMemory)
    }

    fun invalidateData(key: String) {
        cache?.invalidateData(key)
    }

    private data class CacheNode(
        val current: DistributedCache,
        val next: CacheNode?
    ) {

        fun getData(key: String, duration: Duration, onlyInMemory: Boolean): EncryptedData? {
            return if (shouldExecute(onlyInMemory, current.isInMemory())) {
                val currentCachedData = current.getData(key, duration)

                if (currentCachedData != null) return currentCachedData

                val nextLevelCachedData = next?.getData(key, duration, onlyInMemory)
                if (nextLevelCachedData != null) current.putData(key, nextLevelCachedData, duration)

                nextLevelCachedData
            } else null
        }

        fun putData(key: String, value: EncryptedData, duration: Duration, onlyInMemory: Boolean) {
            if (shouldExecute(onlyInMemory, current.isInMemory())) {
                current.putData(key, value, duration)
                next?.putData(key, value, duration, onlyInMemory)
            }
        }

        fun invalidateData(key: String) {
            current.invalidateData(key)
            next?.invalidateData(key)
        }

        private fun shouldExecute(onlyInMemory: Boolean, isInMemory: Boolean): Boolean {
            return !onlyInMemory || onlyInMemory && isInMemory
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CacheService::class.java)
    }
}

fun DistributedCache.isInMemory() = this is InMemoryDistributedCache
