package br.com.guiabolso.connector.event

import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent

interface EventDispatcher {

    fun sendEvent(event: RequestEvent): ResponseEvent
}
