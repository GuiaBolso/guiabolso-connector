package br.com.guiabolso.connector.configuration

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@GoogleCloud
@Configuration
class DatastoreConfiguration {

    @Bean
    fun datastore(): Datastore = DatastoreOptions.getDefaultInstance().service
}
