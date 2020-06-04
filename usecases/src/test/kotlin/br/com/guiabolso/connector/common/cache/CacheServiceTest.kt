package br.com.guiabolso.connector.common.cache

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.event.misc.EasyRandomWrapper.nextObject
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CacheServiceTest {

    private lateinit var memoryCache1: InMemoryDistributedCache
    private lateinit var memoryCache2: InMemoryDistributedCache
    private lateinit var nonMemoryCache1: MultiLevelDistributedCache
    private lateinit var nonMemoryCache2: MultiLevelDistributedCache
    private lateinit var memoryCaches: List<InMemoryDistributedCache>
    private lateinit var nonMemoryCaches: List<MultiLevelDistributedCache>
    private lateinit var service: CacheService

    @BeforeEach
    fun setUp() {
        memoryCache1 = mock()
        whenever(memoryCache1.level).thenReturn(1)

        memoryCache2 = mock()
        whenever(memoryCache2.level).thenReturn(2)

        nonMemoryCache1 = mock()
        whenever(nonMemoryCache1.level).thenReturn(1)

        nonMemoryCache2 = mock()
        whenever(nonMemoryCache2.level).thenReturn(2)

        memoryCaches = listOf(memoryCache1, memoryCache2)
        nonMemoryCaches = listOf(nonMemoryCache1, nonMemoryCache2)

        service = CacheService(memoryCaches.plus(nonMemoryCaches))
    }

    @Test
    fun `should return null when none of the caches have the key`() {
        val key = nextObject<String>()
        val duration = nextObject<Duration>()
        val onlyInMemory = false

        memoryCaches.plus(nonMemoryCaches).forEach {
            whenever(it.getData(key, duration)).thenReturn(null)
        }

        val actual = service.getData(key, duration, onlyInMemory)

        assertThat(actual).isNull()

        memoryCaches.plus(nonMemoryCaches).forEach {
            verify(it).getData(key, duration)
        }
    }

    @Test
    fun `should return data from lowest level memory without putting data in the higher level ones`() {
        val key = nextObject<String>()
        val duration = nextObject<Duration>()
        val onlyInMemory = true
        val value = nextObject<EncryptedData>()

        whenever(memoryCache1.getData(key, duration)).thenReturn(value)

        val actual = service.getData(key, duration, onlyInMemory)

        assertThat(actual).isEqualTo(value)

        verify(memoryCache1).getData(key, duration)

        nonMemoryCaches.plus(memoryCache2).forEach {
            verify(it, never()).getData(key, duration)
            verify(it, never()).putData(key, value, duration)
        }
    }

    @Test
    fun `should return data from higher level memory cache and put data in the lower level memory caches`() {
        val key = nextObject<String>()
        val duration = nextObject<Duration>()
        val onlyInMemory = true
        val value = nextObject<EncryptedData>()

        whenever(memoryCache1.getData(key, duration)).thenReturn(null)
        whenever(memoryCache2.getData(key, duration)).thenReturn(value)

        val actual = service.getData(key, duration, onlyInMemory)

        assertThat(actual).isEqualTo(value)

        memoryCaches.forEach {
            verify(it).getData(key, duration)
        }

        verify(memoryCache1).putData(key, value, duration)

        nonMemoryCaches.forEach {
            verify(it, never()).getData(key, duration)
            verify(it, never()).putData(key, value, duration)
        }
    }

    @Test
    fun `should return data from higher level non memory cache and put data in all previous`() {
        val key = nextObject<String>()
        val duration = nextObject<Duration>()
        val onlyInMemory = false
        val value = nextObject<EncryptedData>()

        memoryCaches.plus(nonMemoryCache1).forEach {
            whenever(it.getData(key, duration)).thenReturn(null)
        }

        whenever(nonMemoryCache2.getData(key, duration)).thenReturn(value)

        val actual = service.getData(key, duration, onlyInMemory)

        assertThat(actual).isEqualTo(value)

        memoryCaches.plus(nonMemoryCaches).forEach {
            verify(it).getData(key, duration)
        }

        memoryCaches.plus(nonMemoryCache1).forEach {
            verify(it).putData(key, value, duration)
        }
    }

    @Test
    fun `should put data in memory caches only`() {
        val key = nextObject<String>()
        val duration = nextObject<Duration>()
        val onlyInMemory = true
        val value = nextObject<EncryptedData>()

        service.putData(key, value, duration, onlyInMemory)

        memoryCaches.forEach {
            verify(it).putData(key, value, duration)
        }

        nonMemoryCaches.forEach {
            verify(it, never()).putData(key, value, duration)
        }
    }

    @Test
    fun `should put data in all caches`() {
        val key = nextObject<String>()
        val duration = nextObject<Duration>()
        val onlyInMemory = false
        val value = nextObject<EncryptedData>()

        service.putData(key, value, duration, onlyInMemory)

        memoryCaches.plus(nonMemoryCaches).forEach {
            verify(it).putData(key, value, duration)
        }
    }

    @Test
    fun `should invalidate data in all caches`() {
        val key = nextObject<String>()

        service.invalidateData(key)

        memoryCaches.plus(nonMemoryCaches).forEach {
            verify(it).invalidateData(key)
        }
    }
}
