package br.com.guiabolso.connector.common.credentials

import br.com.guiabolso.connector.common.cryptography.DecryptService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.utils.toUTF8String
import br.com.guiabolso.connector.configuration.ConfigService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ClientCredentialsConfiguration(
    configService: ConfigService,
    @Qualifier("configDecryptorService") private val decryptService: DecryptService
) : ClientCredentialsProvider {

    private val clientId = configService.getRequiredString("client.id")
    private val clientSecret = configService.getRequiredString("client.secret")

    @Bean
    override fun clientCredentials() = ClientCredentials(
        clientId = decryptService.decrypt(EncryptedData(clientId.toByteArray())).toUTF8String(),
        clientSecret = decryptService.decrypt(EncryptedData(clientSecret.toByteArray())).toUTF8String()
    )
}
