package br.com.guiabolso.connector.storage

import br.com.guiabolso.connector.common.cache.MultiLevelDistributedCache
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.exception.code.AwsErrorCode.S3_STORAGE_FAILURE
import br.com.guiabolso.connector.common.time.ZonedDateTimeProvider
import br.com.guiabolso.connector.configuration.Aws
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.StorageException
import com.amazonaws.AmazonServiceException
import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.S3Object
import java.time.Duration
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Service

@Aws
@Service
class S3Service(
    configService: ConfigService,
    private val timeProvider: ZonedDateTimeProvider,
    private val amazonS3: AmazonS3
) : MultiLevelDistributedCache {

    private val bucket = configService.getRequiredString("s3.bucket.name")
    private val defaultExpirationInMinutes = configService.getRequiredString("s3.expire.duration.minutes").toLong()

    override val level = 1

    override fun getData(key: String, duration: Duration): EncryptedData? {
        return try {
            amazonS3.getObject(bucket, key).use { s3Object ->
                when {
                    s3Object.isExpiredBy(duration) -> null
                    else -> EncryptedData(s3Object.objectContent.readBytes())
                }
            }
        } catch (e: AmazonServiceException) {
            if (e.statusCode == NOT_FOUND) return null

            throw StorageException(
                S3_STORAGE_FAILURE,
                "Could not get data in Amazon S3 due to the following exception",
                e
            )
        } catch (e: SdkClientException) {
            throw StorageException(
                S3_STORAGE_FAILURE,
                "Could not get data in Amazon S3 due to the following exception",
                e
            )
        }
    }

    override fun putData(key: String, value: EncryptedData, duration: Duration) {
        val contentStream = value.value.inputStream()
        val metadata = ObjectMetadata().apply {
            contentLength = value.value.size.toLong()
            contentType = "application/json"
        }

        val request = PutObjectRequest(bucket, key, contentStream, metadata)

        try {
            amazonS3.putObject(request)
        } catch (ex: SdkClientException) {
            throw StorageException(
                S3_STORAGE_FAILURE,
                "Could not put data in Amazon S3 due to the following exception",
                ex
            )
        }
    }

    override fun invalidateData(key: String) {
        try {
            amazonS3.deleteObject(bucket, key)
        } catch (e: SdkClientException) {
            throw StorageException(
                S3_STORAGE_FAILURE,
                "Could not invalidate data in Amazon S3 due to the following exception",
                e
            )
        }
    }

    private fun S3Object.isExpiredBy(currentDuration: Duration): Boolean {
        val currentMinutes = currentDuration.toMinutes()
        val expirationInMinutes =
            if (currentMinutes <= defaultExpirationInMinutes) currentMinutes else defaultExpirationInMinutes

        val limit = this.objectMetadata.lastModified.toInstant().plus(expirationInMinutes, ChronoUnit.MINUTES)
        return timeProvider.now().toInstant().isAfter(limit)
    }

    companion object {
        private const val NOT_FOUND = 404
    }
}
