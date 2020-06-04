package br.com.guiabolso.connector.event.exception

import br.com.guiabolso.connector.common.code.ErrorCode.MISSING_REQUIRED_PARAMETER
import br.com.guiabolso.events.model.EventErrorType.BadRequest

class MissingRequiredParameterException(parameter: String) : EventException(
    code = MISSING_REQUIRED_PARAMETER,
    parameters = mapOf("name" to parameter),
    type = BadRequest
)
