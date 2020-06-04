package br.com.guiabolso.connector.configuration

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@GoogleCloud
@Configuration
class CloudStorageConfiguration {

    @Bean
    fun cloudStorageClient(): Storage = StorageOptions.getDefaultInstance().service
}
