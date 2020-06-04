package br.com.guiabolso.connector.datapackage

import br.com.guiabolso.connector.datapackage.loader.DataPackageConfigurationLoader
import br.com.guiabolso.connector.datapackage.model.DataPackageConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataPackageBeanConfiguration {

    @Bean
    fun dataPackageConfiguration(configurationLoader: DataPackageConfigurationLoader): DataPackageConfiguration {
        return configurationLoader.load()
    }
}
