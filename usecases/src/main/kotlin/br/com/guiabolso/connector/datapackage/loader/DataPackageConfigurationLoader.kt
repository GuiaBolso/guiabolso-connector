package br.com.guiabolso.connector.datapackage.loader

import br.com.guiabolso.connector.datapackage.model.DataPackageConfiguration

interface DataPackageConfigurationLoader {

    fun load(): DataPackageConfiguration
}
