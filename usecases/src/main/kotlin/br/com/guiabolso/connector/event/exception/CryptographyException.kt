package br.com.guiabolso.connector.event.exception

import br.com.guiabolso.events.model.EventErrorType.Generic

class CryptographyException(code: String, message: String, cause: Throwable? = null) : EventException(
    code = code,
    parameters = mapOf("message" to message),
    type = Generic,
    cause = cause
)
