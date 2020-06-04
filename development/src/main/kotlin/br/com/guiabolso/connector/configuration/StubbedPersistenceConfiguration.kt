package br.com.guiabolso.connector.configuration

import br.com.guiabolso.connector.common.profile.Development
import br.com.guiabolso.connector.persistence.token.StubbedTable
import br.com.guiabolso.events.json.MapperHolder.mapper
import com.google.gson.JsonArray
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Development
@Configuration
@PropertySource("classpath:/conf/development.properties")
class StubbedPersistenceConfiguration(
    private val configService: ConfigService
) {

    @Bean
    fun database(): InMemoryDatabase {
        val jsonFile = configService.loadResource(
            filePath = configService.getRequiredString("initdatabase.path")
        ).bufferedReader()

        val tables = mapper.fromJson(jsonFile, JsonArray::class.java)

        return mutableMapOf<String, MutableSet<StubbedTable>>().apply {
            StubbedTable::class.nestedClasses.forEach { _class ->
                val table = _class.simpleName!!
                this[table] = mutableSetOf()

                tables.map { it.asJsonObject[table] }.firstOrNull()?.asJsonArray?.forEach {
                    val row = mapper.fromJson(it, _class.javaObjectType)
                    this[table]?.add(row as StubbedTable)
                }

                this[table]
            }
        }
    }
}

typealias InMemoryDatabase = MutableMap<String, MutableSet<StubbedTable>>

@Suppress("UNCHECKED_CAST")
inline fun <reified T : StubbedTable> InMemoryDatabase.findAllByType() =
    this[T::class.java.simpleName]!! as MutableSet<T>
