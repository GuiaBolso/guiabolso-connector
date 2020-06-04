package br.com.guiabolso.connector.cache

import br.com.guiabolso.connector.common.cache.InMemoryDistributedCache
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.configuration.Redis
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service

@Redis
@Service
class RedisService(
    configService: ConfigService,
    private val client: RedissonClient
) : InMemoryDistributedCache {

    private val defaultExpirationInMinutes = configService.getRequiredString("redis.expire.duration.minutes").toLong()

    override val level = 1

    override fun getData(key: String, duration: Duration): EncryptedData? {
        val expirationInMinutes = getExpiration(duration)
        return client.getBucket<ByteArray>(key).run {
            val value = get() ?: return null
            EncryptedData(value = getAndSet(value, expirationInMinutes, TimeUnit.MINUTES))
        }
    }

    override fun putData(key: String, value: EncryptedData, duration: Duration) {
        val expirationInMinutes = getExpiration(duration)
        client.getBucket<ByteArray>(key).run {
            set(value.value, expirationInMinutes, TimeUnit.MINUTES)
        }
    }

    override fun invalidateData(key: String) {
        client.getBucket<ByteArray>(key).delete()
    }

    private fun getExpiration(currentDuration: Duration): Long {
        val currentMinutes = currentDuration.toMinutes()
        return if (currentMinutes <= defaultExpirationInMinutes) currentMinutes else defaultExpirationInMinutes
    }
}
