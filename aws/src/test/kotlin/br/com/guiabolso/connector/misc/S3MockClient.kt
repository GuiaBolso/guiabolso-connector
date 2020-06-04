package br.com.guiabolso.connector.misc

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.findify.s3mock.S3Mock

object S3MockClient {

    init {
        S3Mock.Builder().withPort(8001).withInMemoryBackend().build().start()
    }

    private val client = AmazonS3ClientBuilder
        .standard()
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration("http://localhost:8001", "sa-east-1"))
        .build()

    fun getClient(bucket: String): AmazonS3 {
        try {
            client.deleteBucket(bucket)
        } catch (ignored: Exception) {
        }
        client.createBucket(bucket)
        return client
    }
}
