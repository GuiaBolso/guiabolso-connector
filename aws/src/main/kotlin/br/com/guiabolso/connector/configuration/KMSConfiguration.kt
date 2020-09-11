package br.com.guiabolso.connector.configuration

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.caching.CachingCryptoMaterialsManager
import com.amazonaws.encryptionsdk.caching.LocalCryptoMaterialsCache
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider
import com.amazonaws.services.kms.AWSKMS
import com.amazonaws.services.kms.AWSKMSClientBuilder
import java.util.concurrent.TimeUnit
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Aws
@Configuration
@PropertySource("classpath:/conf/aws.properties")
class KMSConfiguration(configService: ConfigService) {

    private val encryptionKey = configService.getRequiredString("kms.encryption.key")
    private val cacheCapacity = configService.getRequiredInt("kms.cache.capacity")
    private val cacheMaxAgeInMinutes = configService.getRequiredInt("kms.cache.maxAgeMinutes").toLong()
    private val cacheMessageUseLimit = configService.getRequiredInt("kms.cache.messageUseLimit").toLong()

    @Bean
    fun kmsCredentials(configService: ConfigService) = KMSCredentials(
        encryptionKey = encryptionKey,
        serviceEndpoint = configService.getRequiredString("kms.service.endpoint"),
        signingRegion = configService.getRequiredString("kms.signing.region")
    )

    @Bean
    fun kmsClient(kmsCredentials: KMSCredentials): AWSKMS {
        return AWSKMSClientBuilder.standard()!!.apply {
            setEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(
                    kmsCredentials.serviceEndpoint,
                    kmsCredentials.signingRegion
                )
            )
        }.build()!!
    }

    @Bean
    fun crypto() = AwsCrypto()

    @Bean
    fun cryptoCache() = CachingCryptoMaterialsManager.newBuilder()
        .withMasterKeyProvider(KmsMasterKeyProvider.builder().build().getMasterKey(encryptionKey))
        .withCache(LocalCryptoMaterialsCache(cacheCapacity))
        .withMaxAge(cacheMaxAgeInMinutes, TimeUnit.MINUTES)
        .withMessageUseLimit(cacheMessageUseLimit)
        .build()!!

    data class KMSCredentials(
        val encryptionKey: String,
        val serviceEndpoint: String,
        val signingRegion: String
    )
}
