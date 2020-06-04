package br.com.guiabolso.connector.event.exception

import br.com.guiabolso.events.model.RedirectPayload
import br.com.guiabolso.events.model.RequestEvent

class RedirectException(
    event: RequestEvent,
    val payload: RedirectPayload,
    cause: Throwable? = null
) : RuntimeException("Redirecting ${event.name}:V${event.version} to ${payload.url}", cause)
