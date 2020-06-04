package br.com.guiabolso.connector.event.exception

import br.com.guiabolso.events.model.EventErrorType
import br.com.guiabolso.events.model.EventErrorType.Generic

open class EventException(
    val code: String,
    val parameters: Map<String, Any?>,
    val type: EventErrorType = Generic,
    cause: Throwable? = null
) : RuntimeException(code, cause)
