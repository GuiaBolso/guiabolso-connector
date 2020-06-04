package br.com.guiabolso.connector.wrapper

import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import br.com.guiabolso.events.server.handler.EventHandler
import java.lang.String.format
import org.slf4j.LoggerFactory

class LoggingEventHandlerWrapper(private val eventHandler: EventHandler) : EventHandler {

    override val eventName = eventHandler.eventName
    override val eventVersion = eventHandler.eventVersion
    private val logger = LoggerFactory.getLogger(eventHandler.javaClass)

    override fun handle(event: RequestEvent): ResponseEvent {
        logger.info("Starting event process.")
        val start = System.nanoTime()
        try {
            return eventHandler.handle(event)
        } finally {
            logger.info("Event process finished in ${toString(System.nanoTime() - start)}.")
        }
    }

    private fun toString(nanoSeconds: Long) = when {
        nanoSeconds >= SECONDS -> "${(nanoSeconds / SECONDS).format()}s"
        nanoSeconds >= MILLI -> "${(nanoSeconds / MILLI).format()}ms"
        nanoSeconds >= MICRO -> "${(nanoSeconds / MICRO).format()}µs"
        else -> "${nanoSeconds}ns"
    }

    private fun Double.format() = format("%.${2}f", this)

    companion object {
        private const val SECONDS: Double = 10e9
        private const val MILLI: Double = 10e6
        private const val MICRO: Double = 10e3
    }
}
