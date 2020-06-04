package br.com.guiabolso.connector.configuration

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
@PropertySource("classpath:/conf/application.properties")
class ConfigService(
    private val env: Environment
) {

    fun getString(property: String, default: String) = getProperty(property) ?: default

    fun getRequiredString(property: String) = getRequiredProperty(property)

    fun getInt(property: String, default: Int) = getProperty(property)?.toInt() ?: default

    fun getRequiredInt(property: String) = getRequiredProperty(property).toInt()

    fun getBoolean(property: String, default: Boolean) = getProperty(property)?.toBoolean() ?: default

    fun getRequiredBoolean(property: String) = getRequiredProperty(property).toBoolean()

    fun loadResource(filePath: String): InputStream {
        return File(filePath).let { file ->
            if (file.isFile) {
                FileInputStream(file)
            } else {
                Thread.currentThread().contextClassLoader.getResourceAsStream(filePath)
                    ?: throw IllegalStateException("Could not load data path input stream from path: $filePath")
            }
        }
    }

    private fun getRequiredProperty(property: String): String {
        return getProperty(property)
            ?: throw IllegalArgumentException("Required configuration property '$property' not found.")
    }

    private fun getProperty(property: String): String? {
        val value = env.getProperty(toEnvConvention(property))
        if (value != null) {
            logger.info("Config '$property' overwritten with '${toEnvConvention(property)}'.")
            return value
        }

        return env.getProperty(property)
    }

    private fun toEnvConvention(property: String) = property.toUpperCase().replace(".", "_")

    companion object {
        private val logger = LoggerFactory.getLogger(ConfigService::class.java)!!
    }
}
