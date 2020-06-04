package br.com.guiabolso.connector.common.exception

import br.com.guiabolso.connector.common.exception.code.AwsErrorCode.KMS_DECRYPTION_FAILURE
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.events.model.EventErrorType.Generic

class DecryptionException(message: String, cause: Throwable? = null) : EventException(
    code = KMS_DECRYPTION_FAILURE,
    parameters = mapOf("message" to message),
    type = Generic,
    cause = cause
)
