package br.com.guiabolso.connector.configuration

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Aws
@Configuration
class S3Configuration(
    configService: ConfigService
) {

    private val region = configService.getRequiredString("s3.signing.region")
    private val endpoint = configService.getRequiredString("s3.service.endpoint")

    @Bean
    fun s3Client() = AmazonS3ClientBuilder
        .standard()
        .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(endpoint, region))
        .build()!!
}
