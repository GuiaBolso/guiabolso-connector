package br.com.guiabolso.connector.storage

import br.com.guiabolso.connector.GcpErrorCode.CLOUD_STORAGE_FAILURE
import br.com.guiabolso.connector.common.cache.MultiLevelDistributedCache
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.configuration.GoogleCloud
import br.com.guiabolso.connector.event.exception.StorageException
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException as GCloudStorageException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Service

@Service
@GoogleCloud
class CloudStorageService(
    configService: ConfigService,
    private val timeProvider: ZonedDateTimeProvider,
    private val storage: Storage
) : MultiLevelDistributedCache {
    override val level = 1

    private val bucket = configService.getRequiredString("storage.bucket.name")
    private val defaultExpirationInMinutes = configService.getRequiredString("storage.expire.duration.minutes").toLong()

    override fun getData(key: String, duration: Duration): EncryptedData? {
        return try {
            val blob = storage.get(BlobId.of(bucket, key)) ?: return null
            if (!isExpired(blob.updateTime, duration)) {
                EncryptedData(blob.getContent())
            } else null
        } catch (e: GCloudStorageException) {
            throw StorageException(CLOUD_STORAGE_FAILURE, "Could not fetch data from cloud storage", e)
        }
    }

    override fun putData(key: String, value: EncryptedData, duration: Duration) {
        val blobId = BlobId.of(bucket, key)
        val blobInfo = BlobInfo.newBuilder(blobId).build()
        try {
            storage.create(blobInfo, value.value)
        } catch (e: GCloudStorageException) {
            throw StorageException(CLOUD_STORAGE_FAILURE, "Could not put data into cloud storage", e)
        }
    }

    override fun invalidateData(key: String) {
        val blobId = BlobId.of(bucket, key)
        try {
            storage.delete(blobId)
        } catch (e: GCloudStorageException) {
            throw StorageException(CLOUD_STORAGE_FAILURE, "Could not delete data from cloud storage", e)
        }
    }

    private fun isExpired(objectUpdateTime: Long, currentDuration: Duration): Boolean {
        val currentMinutes = currentDuration.toMinutes()
        val expirationInMinutes =
            if (currentMinutes <= defaultExpirationInMinutes) currentMinutes else defaultExpirationInMinutes

        val limit = Instant.ofEpochMilli(objectUpdateTime).plus(expirationInMinutes, ChronoUnit.MINUTES)
        return timeProvider.now().toInstant().isAfter(limit)
    }
}
