package br.com.guiabolso.connector.common.failure

import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedPolicy.ALWAYS
import br.com.guiabolso.connector.common.failure.RedirectOnUnauthorizedPolicy.NEVER
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.events.model.EventErrorType
import br.com.guiabolso.events.model.RedirectPayload
import br.com.guiabolso.events.model.RequestEvent
import org.springframework.stereotype.Service

@Service
class RedirectOnUnauthorizedService(
    configService: ConfigService
) {
    private val activePolicy: RedirectOnUnauthorizedPolicy
    private val gbConnectUrl = configService.getRequiredString("gbconnect.url")

    init {
        val activePolicyName = configService.getString("redirect.unauthorized.policy", NEVER.name)
        activePolicy = RedirectOnUnauthorizedPolicy.valueOf(activePolicyName.toUpperCase())
    }

    fun maybeRedirectFor(event: RequestEvent, throwable: Throwable?, policy: RedirectOnUnauthorizedPolicy) {
        if (shouldHandleError(policy) && throwable is EventException && throwable.type == EventErrorType.Unauthorized)
            throw RedirectException(event, RedirectPayload(gbConnectUrl), throwable)
    }

    private fun shouldHandleError(neededPolicy: RedirectOnUnauthorizedPolicy): Boolean {
        return if (activePolicy == NEVER) false
        else activePolicy == ALWAYS || neededPolicy == activePolicy
    }
}
