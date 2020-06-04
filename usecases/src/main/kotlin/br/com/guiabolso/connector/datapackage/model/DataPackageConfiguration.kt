package br.com.guiabolso.connector.datapackage.model

data class DataPackageConfiguration(
    val dataPackages: List<DataPackage>,
    val dataCaches: List<DataCache>
)
