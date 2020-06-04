package br.com.guiabolso.connector.token.exception

import br.com.guiabolso.connector.common.code.ErrorCode.EXPIRED_AUTHORIZATION_CODE
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.events.model.EventErrorType.Expired

class ExpiredAuthorizationCodeException(parameters: Map<String, Any?> = emptyMap()) : EventException(
    code = EXPIRED_AUTHORIZATION_CODE,
    parameters = parameters,
    type = Expired
)
