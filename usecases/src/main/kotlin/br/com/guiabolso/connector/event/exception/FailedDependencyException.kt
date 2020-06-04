package br.com.guiabolso.connector.event.exception

import br.com.guiabolso.connector.common.code.ErrorCode.FAILED_DEPENDENCY
import br.com.guiabolso.events.model.EventErrorType

class FailedDependencyException(cause: Throwable? = null) : EventException(
    code = FAILED_DEPENDENCY,
    parameters = emptyMap(),
    type = EventErrorType.Generic,
    cause = cause
)
