package br.com.guiabolso.connector.datapackage.model

data class DataPackage(
    val publish: PublishConfiguration,
    val sources: List<PackageSource>
) {
    init {
        require(sources.distinctBy { it.statusKey }.size == sources.size) {
            "Duplicated source status key found in ${publish.name} configuration."
        }
    }
}
