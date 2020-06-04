package br.com.guiabolso.connector.cache

import br.com.guiabolso.connector.AbstractSpringTestCase
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles(profiles = ["redis"])
class RedisServiceTest : AbstractSpringTestCase() {

    @Autowired
    private lateinit var configService: ConfigService

    @Autowired
    private lateinit var client: RedissonClient

    private lateinit var service: RedisService

    @BeforeEach
    fun setUp() {
        service = RedisService(configService, client)
    }

    @Test
    fun `should return data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = Duration.ofMinutes(1)

        service.putData(key, value, duration)

        val actual = service.getData(key, duration)!!

        assertThat(actual).isEqualTo(value)
    }

    @Test
    fun `should return null if key not exists`() {
        val key = nextObject<String>()
        val duration = Duration.ofMinutes(30)

        val actual = service.getData(key, duration)

        assertThat(actual).isNull()
    }

    @Test
    fun `should put data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = Duration.ofMinutes(1)

        service.putData(key, value, duration)

        val actual = service.getData(key, duration)!!

        assertThat(actual).isEqualTo(value)
    }

    @Test
    fun `should invalidate data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = Duration.ofMinutes(1)

        service.putData(key, value, duration)

        service.invalidateData(key)

        val actual = service.getData(key, duration)

        assertThat(actual).isNull()
    }
}
