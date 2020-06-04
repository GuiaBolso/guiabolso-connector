package br.com.guiabolso.connector.event.misc

import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.context.EventContextHolder
import br.com.guiabolso.events.json.MapperHolder.mapper
import br.com.guiabolso.events.model.RequestEvent
import java.util.UUID

fun buildEvent(
    name: String? = "some:event",
    version: Int? = 1,
    userId: String? = null,
    payload: Map<Any, Any>? = null
): RequestEvent = EventBuilder.event {
    this.name = name
    this.version = version
    this.id = EventContextHolder.getContext()?.id ?: UUID.randomUUID().toString()
    this.flowId = EventContextHolder.getContext()?.flowId ?: UUID.randomUUID().toString()
    this.identity = if (userId.isNullOrEmpty()) emptyMap() else mapOf("userId" to userId)
    this.payload = if (payload == null) null else mapper.toJsonTree(payload).asJsonObject
}
