package br.com.guiabolso.connector.event.model

sealed class EventVersion {
    object AllVersions : EventVersion()
    data class ExactVersion(val number: Int) : EventVersion()
}
