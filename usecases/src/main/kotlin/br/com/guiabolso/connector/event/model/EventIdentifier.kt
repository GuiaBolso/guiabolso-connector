package br.com.guiabolso.connector.event.model

data class EventIdentifier(
    val name: String,
    val version: Int
) {
    override fun toString() = "$name:V$version"
}
