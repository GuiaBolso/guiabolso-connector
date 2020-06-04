package br.com.guiabolso.connector.datapackage.model

data class PublishConfiguration(
    val type: PackageType,
    val name: String,
    val version: Int
)
