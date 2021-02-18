package br.com.guiabolso.connector.datapackage.model

data class UtsVariable(
    val key: String,
    val value: Any?,
    val type: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtsVariable

        return key == other.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
