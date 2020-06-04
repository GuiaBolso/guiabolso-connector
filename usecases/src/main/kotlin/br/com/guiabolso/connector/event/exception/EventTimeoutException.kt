package br.com.guiabolso.connector.event.exception

import br.com.guiabolso.connector.common.code.ErrorCode.TIMEOUT
import br.com.guiabolso.events.model.EventErrorType.Generic

class EventTimeoutException : EventException(
    code = TIMEOUT,
    parameters = emptyMap(),
    type = Generic
)
