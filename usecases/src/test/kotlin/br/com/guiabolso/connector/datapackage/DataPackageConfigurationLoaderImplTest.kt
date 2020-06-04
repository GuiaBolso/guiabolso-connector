package br.com.guiabolso.connector.datapackage

import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.configuration.YAMLConfiguration
import br.com.guiabolso.connector.datapackage.loader.DataPackageConfigurationLoaderImpl
import br.com.guiabolso.connector.datapackage.model.PackageType
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment

class DataPackageConfigurationLoaderImplTest {

    private lateinit var environment: Environment
    private lateinit var yamlMapper: YAMLMapper
    private lateinit var loader: DataPackageConfigurationLoaderImpl

    @BeforeEach
    internal fun setUp() {
        environment = mock()
        yamlMapper = YAMLConfiguration().yamlObjectMapper()
        loader = DataPackageConfigurationLoaderImpl(
            configService = ConfigService(environment),
            yamlMapper = yamlMapper
        )
    }

    @Test
    internal fun `test can load datapackage`() {
        whenever(environment.getProperty("datapackages.path")).thenReturn("data_packages_test.yaml")

        val dataPackageConfiguration = loader.load()
        assertEquals(1, dataPackageConfiguration.dataPackages.size)

        val dataPackage = dataPackageConfiguration.dataPackages.first()
        assertEquals("guiabolso-connector:variables", dataPackage.publish.name)
        assertEquals(1, dataPackage.publish.version)
        assertEquals(PackageType.EVENT, dataPackage.publish.type)

        assertEquals(2, dataPackage.sources.size)

        assertEquals("GBCONNECT.CREDIT.SCORES.STATUS", dataPackage.sources.first().statusKey)
        assertEquals("guiabolso-connector:user:credit:scores", dataPackage.sources.first().eventName)
        assertEquals(1, dataPackage.sources.first().eventVersion)

        assertEquals("GBCONNECT.CREDIT.TRANSACTIONS.VARIABLES.STATUS", dataPackage.sources.last().statusKey)
        assertEquals("guiabolso-connector:user:transactions:variables", dataPackage.sources.last().eventName)
        assertEquals(1, dataPackage.sources.last().eventVersion)
    }

    @Test
    internal fun `test cannot load datapackage with duplicated status key`() {
        whenever(environment.getProperty("datapackages.path")).thenReturn("data_packages_test_duplicated_key.yaml")

        assertThatThrownBy { loader.load() }
            .hasCauseExactlyInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Duplicated source status key found")
    }
}
