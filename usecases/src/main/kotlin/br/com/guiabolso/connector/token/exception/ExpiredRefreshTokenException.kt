package br.com.guiabolso.connector.token.exception

import br.com.guiabolso.connector.common.code.ErrorCode.EXPIRED_REFRESH_TOKEN
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.events.model.EventErrorType.Expired

class ExpiredRefreshTokenException : EventException(
    code = EXPIRED_REFRESH_TOKEN,
    parameters = emptyMap(),
    type = Expired
)
