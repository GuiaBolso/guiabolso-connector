package br.com.guiabolso.connector.event.exception

import br.com.guiabolso.connector.common.code.ErrorCode.CACHE_CONFIGURATION_ERROR
import br.com.guiabolso.events.model.EventErrorType

class EventCacheConfigurationException(message: String? = null) : EventException(
    code = CACHE_CONFIGURATION_ERROR,
    parameters = mapOf("message" to message),
    type = EventErrorType.Generic
)
