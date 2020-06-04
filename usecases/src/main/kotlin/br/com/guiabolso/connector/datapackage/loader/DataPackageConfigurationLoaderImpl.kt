package br.com.guiabolso.connector.datapackage.loader

import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.datapackage.model.DataPackageConfiguration
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:/conf/data_packages.properties")
class DataPackageConfigurationLoaderImpl(
    private val configService: ConfigService,
    private val yamlMapper: YAMLMapper
) : DataPackageConfigurationLoader {

    override fun load(): DataPackageConfiguration = yamlMapper.readValue(
        configService.loadResource(configService.getRequiredString("datapackages.path"))
    )
}
