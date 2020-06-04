package br.com.guiabolso.connector.configuration

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class YAMLConfiguration {

    @Bean
    fun yamlObjectMapper(): YAMLMapper {
        return YAMLMapper().apply {
            registerModule(KotlinModule())
        }
    }
}
