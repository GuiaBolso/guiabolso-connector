package br.com.guiabolso.connector.event.misc

import br.com.guiabolso.connector.event.exception.MissingRequiredParameterException
import com.google.gson.JsonElement
import com.google.gson.JsonObject

fun <T> T?.required(name: String): T {
    return this ?: throw MissingRequiredParameterException(name)
}

fun JsonElement?.requiredString(name: String): String {
    this as? JsonObject ?: throw IllegalArgumentException("Cannot get property $name from json element $this")
    return this.get(name)?.asString ?: throw MissingRequiredParameterException(name)
}
