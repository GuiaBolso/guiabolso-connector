package br.com.guiabolso.connector.token.exception

import br.com.guiabolso.connector.common.code.ErrorCode.UNAUTHORIZED_USER
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.events.model.EventErrorType.Unauthorized

class UnauthorizedUserException(cause: Throwable? = null) : EventException(
    code = UNAUTHORIZED_USER,
    parameters = emptyMap(),
    type = Unauthorized,
    cause = cause
)
