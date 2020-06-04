package br.com.guiabolso.connector.configuration

import com.google.cloud.kms.v1.CryptoKeyPathName
import com.google.cloud.kms.v1.KeyManagementServiceClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@GoogleCloud
@Configuration
class CloudKMSConfiguration(
    private val configService: ConfigService
) {

    @Bean
    fun kmsClient(): KeyManagementServiceClient = KeyManagementServiceClient.create()

    @Bean
    fun resourceName(): CryptoKeyPathName = CryptoKeyPathName.of(
        configService.getRequiredString("cloud.kms.project"),
        configService.getRequiredString("cloud.kms.location"),
        configService.getRequiredString("cloud.kms.keyring"),
        configService.getRequiredString("cloud.kms.cryptokey")
    )
}
