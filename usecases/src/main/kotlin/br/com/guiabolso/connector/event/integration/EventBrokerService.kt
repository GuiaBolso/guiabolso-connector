package br.com.guiabolso.connector.event.integration

import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.exception.EventTimeoutException
import br.com.guiabolso.connector.event.exception.FailedDependencyException
import br.com.guiabolso.events.client.EventClient
import br.com.guiabolso.events.client.model.Response.Error
import br.com.guiabolso.events.client.model.Response.FailedDependency
import br.com.guiabolso.events.client.model.Response.Redirect
import br.com.guiabolso.events.client.model.Response.Success
import br.com.guiabolso.events.client.model.Response.Timeout
import br.com.guiabolso.events.model.EventMessage
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventBrokerService(
    private val eventClient: EventClient
) {

    fun sendEvent(uri: String, event: RequestEvent, timeout: Int? = null): ResponseEvent {
        logger.info("Sending event ${event.name}:V${event.version} to $uri")

        when (val response =
            eventClient.sendEvent(url = uri, requestEvent = event.cleanUpIdentity(), timeout = timeout)) {
            is Success -> return response.event
            is Redirect -> return response.event
            is Error -> {
                val error = response.event.payloadAs<EventMessage>()
                logger.error("Error sending event ${event.name}:V${event.version} to $uri (error message: $error), error type: ${response.errorType}")
                throw EventException(
                    error.code,
                    error.parameters,
                    response.errorType
                )
            }
            is FailedDependency -> {
                val failedDependencyMessage = "Failed dependency sending event ${event.name}:V${event.version} to $uri"
                logger.error(failedDependencyMessage, response.exception)
                throw FailedDependencyException()
            }
            is Timeout -> {
                val timeoutMessage = "Timeout sending event ${event.name}:V${event.version} to $uri"
                logger.error(timeoutMessage, response.exception)
                throw EventTimeoutException()
            }
        }
    }

    private fun RequestEvent.cleanUpIdentity() = copy(identity = identity.deepCopy().apply { remove("userId") })

    companion object {
        private val logger = LoggerFactory.getLogger(EventBrokerService::class.java)
    }
}
