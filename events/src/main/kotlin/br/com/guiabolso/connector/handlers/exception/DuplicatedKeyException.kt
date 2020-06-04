package br.com.guiabolso.connector.handlers.exception

import br.com.guiabolso.connector.common.code.EventsErrorCode.DUPLICATED_KEY
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.events.model.EventErrorType.Generic

class DuplicatedKeyException(duplicatedKey: String) : EventException(
    code = DUPLICATED_KEY,
    parameters = mapOf("key" to duplicatedKey),
    type = Generic
)
