package br.com.guiabolso.connector.storage

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.misc.EasyRandomWrapper.easyRandom
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.misc.S3MockClient
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import java.io.ByteArrayInputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class S3ServiceTest {

    private lateinit var configService: ConfigService
    private lateinit var timeProvider: ZonedDateTimeProvider
    private lateinit var amazonS3: AmazonS3
    private lateinit var service: S3Service

    @BeforeEach
    fun setUp() {
        configService = mock()
        whenever(configService.getRequiredString(S3_BUCKET_NAME.first)).thenReturn(S3_BUCKET_NAME.second)
        whenever(configService.getRequiredString(S3_EXPIRE_DURATION_MINUTES.first))
            .thenReturn(S3_EXPIRE_DURATION_MINUTES.second)

        timeProvider = mock()

        amazonS3 = S3MockClient.getClient(S3_BUCKET_NAME.second)

        service = S3Service(configService, timeProvider, amazonS3)
    }

    @Test
    fun `should return data`() {
        val key = nextObject<String>()
        val data = nextObject<EncryptedData>()
        val duration = Duration.ofMinutes(1)

        whenever(timeProvider.now()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC))

        service.putData(key, data, duration)

        val actual = service.getData(key, duration)!!

        assertThat(actual.value.contentToString()).isEqualTo(data.value.contentToString())
    }

    @Test
    fun `should return null if key is expired`() {
        val key = nextObject<String>()
        val data = nextObject<EncryptedData>()
        val duration = Duration.ofMinutes(1)

        whenever(timeProvider.now()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(2))

        service.putData(key, data, duration)

        val actual = service.getData(key, duration)

        assertThat(actual).isNull()
    }

    @Test
    fun `should return null if key not exists`() {
        val key = nextObject<String>()
        val duration = Duration.ofMinutes(1)

        val actual = service.getData(key, duration)

        assertThat(actual).isNull()
    }

    @Test
    fun `should put data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = Duration.ofHours(1)

        whenever(timeProvider.now()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC))

        service.putData(key, value, duration)

        val actual = service.getData(key, duration)!!

        assertThat(actual).isEqualTo(value)
    }

    @Test
    fun `should invalidate data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = nextObject<Duration>()

        service.putData(key, value, duration)

        service.invalidateData(key)

        val actual = service.getData(key, duration)

        assertThat(actual).isNull()
    }

    @Test
    fun `should close stream after get object`() {
        val s3Mock: AmazonS3 = mock()
        val s3Object: S3Object = mock()

        val objectMetadata = easyRandom.nextObject(ObjectMetadata::class.java).apply {
            lastModified = Date.from(Instant.now().plus(1, ChronoUnit.DAYS))
        }

        whenever(timeProvider.now()).thenReturn(ZonedDateTime.now())
        whenever(s3Mock.getObject(any<String>(), any<String>())).thenReturn(s3Object)
        whenever(s3Object.objectMetadata).thenReturn(objectMetadata)
        whenever(s3Object.objectContent).thenAnswer {
            S3ObjectInputStream(ByteArrayInputStream(ByteArray(0)), mock())
        }

        val service = S3Service(configService, timeProvider, s3Mock)
        service.getData("any-key", Duration.ofHours(1))

        verify(s3Object).close()
    }

    companion object {
        private val S3_BUCKET_NAME = Pair("s3.bucket.name", "test")
        private val S3_EXPIRE_DURATION_MINUTES = Pair("s3.expire.duration.minutes", "1")
    }
}
