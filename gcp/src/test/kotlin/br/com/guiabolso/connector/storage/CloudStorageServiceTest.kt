package br.com.guiabolso.connector.storage

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.StorageException
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException as GCloudStorageException
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CloudStorageServiceTest {

    private lateinit var configService: ConfigService
    private lateinit var timeProvider: ZonedDateTimeProvider
    private lateinit var storage: Storage
    private lateinit var service: CloudStorageService

    @BeforeEach
    fun setUp() {
        configService = mock()
        whenever(configService.getRequiredString(STORAGE_BUCKET_NAME.first)).thenReturn(STORAGE_BUCKET_NAME.second)
        whenever(configService.getRequiredString(STORAGE_EXPIRE_DURATION_MINUTES.first))
            .thenReturn(STORAGE_EXPIRE_DURATION_MINUTES.second)

        timeProvider = mock()

        storage = mock()

        service = CloudStorageService(configService, timeProvider, storage)
    }

    @Test
    fun `should throw exception when Data Storage service fails to get data`() {
        val key = nextObject<String>()
        val duration = Duration.ofMinutes(1)

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)
        whenever(storage.get(blobId)).thenThrow(GCloudStorageException::class.java)

        assertThatExceptionOfType(StorageException::class.java)
            .isThrownBy { service.getData(key, duration) }

        verify(storage).get(blobId)
        verifyZeroInteractions(timeProvider)
    }

    @Test
    fun `should return data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = Duration.ofMinutes(1)

        val blob = mock<Blob>()
        whenever(blob.getContent()).thenReturn(value.value)
        whenever(blob.updateTime).thenReturn(Instant.now().toEpochMilli())

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)
        whenever(storage.get(blobId)).thenReturn(blob)

        whenever(timeProvider.now()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC))

        val actual = service.getData(key, duration)!!

        assertThat(actual.value.contentToString()).isEqualTo(value.value.contentToString())

        verify(storage).get(blobId)
        verify(timeProvider).now()
    }

    @Test
    fun `should return null if key is expired`() {
        val key = nextObject<String>()
        val duration = Duration.ofMinutes(1)

        val blob = mock<Blob>()
        whenever(blob.updateTime).thenReturn(Instant.now().toEpochMilli())

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)
        whenever(storage.get(blobId)).thenReturn(blob)

        whenever(timeProvider.now()).thenReturn(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(5))

        val actual = service.getData(key, duration)

        assertThat(actual).isNull()

        verify(storage).get(blobId)
        verify(timeProvider).now()
    }

    @Test
    fun `should return null if key not exists`() {
        val key = nextObject<String>()
        val duration = Duration.ofMinutes(1)

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)
        whenever(storage.get(blobId)).thenReturn(null)

        val actual = service.getData(key, duration)

        assertThat(actual).isNull()

        verify(storage).get(blobId)
        verifyZeroInteractions(timeProvider)
    }

    @Test
    fun `should throw exception when Data Storage service fails to put data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = Duration.ofMinutes(1)

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)
        val blobInfo = BlobInfo.newBuilder(blobId).build()

        whenever(storage.create(blobInfo, value.value)).thenThrow(GCloudStorageException::class.java)

        assertThatExceptionOfType(StorageException::class.java)
            .isThrownBy { service.putData(key, value, duration) }

        verify(storage).create(blobInfo, value.value)
        verifyZeroInteractions(timeProvider)
    }

    @Test
    fun `should put data`() {
        val key = nextObject<String>()
        val value = nextObject<EncryptedData>()
        val duration = nextObject<Duration>()

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)
        val blobInfo = BlobInfo.newBuilder(blobId).build()

        service.putData(key, value, duration)

        verify(storage).create(blobInfo, value.value)
        verifyZeroInteractions(timeProvider)
    }

    @Test
    fun `should throw exception when Data Storage service fails to invalidate data`() {
        val key = nextObject<String>()

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)

        whenever(storage.delete(blobId)).thenThrow(GCloudStorageException::class.java)

        assertThatExceptionOfType(StorageException::class.java)
            .isThrownBy { service.invalidateData(key) }

        verify(storage).delete(blobId)
        verifyZeroInteractions(timeProvider)
    }

    @Test
    fun `should invalidate data`() {
        val key = nextObject<String>()

        val blobId = BlobId.of(STORAGE_BUCKET_NAME.second, key)

        service.invalidateData(key)

        verify(storage).delete(blobId)
        verifyZeroInteractions(timeProvider)
    }

    companion object {
        private val STORAGE_BUCKET_NAME = Pair("storage.bucket.name", "test")
        private val STORAGE_EXPIRE_DURATION_MINUTES = Pair("storage.expire.duration.minutes", "1")
    }
}
