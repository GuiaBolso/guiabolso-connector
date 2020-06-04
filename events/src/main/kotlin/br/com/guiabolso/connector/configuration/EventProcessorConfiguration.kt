package br.com.guiabolso.connector.configuration

import br.com.guiabolso.connector.auth.AuthenticationService
import br.com.guiabolso.connector.auth.NoOpAuthenticationService
import br.com.guiabolso.connector.common.tracking.Tracer
import br.com.guiabolso.connector.datapackage.model.DataPackageConfiguration
import br.com.guiabolso.connector.event.EventDispatcher
import br.com.guiabolso.connector.event.cache.CachedEventDispatcher
import br.com.guiabolso.connector.event.cache.EventCacheService
import br.com.guiabolso.connector.event.exception.EventException
import br.com.guiabolso.connector.event.exception.RedirectException
import br.com.guiabolso.connector.handlers.MergingEventHandler
import br.com.guiabolso.connector.proxy.CompositeEventHandlerDiscovery
import br.com.guiabolso.connector.proxy.ProxyingEventHandlerDiscovery
import br.com.guiabolso.connector.wrapper.LoggingEventHandlerWrapper
import br.com.guiabolso.events.builder.EventBuilder.Companion.errorFor
import br.com.guiabolso.events.builder.EventBuilder.Companion.redirectFor
import br.com.guiabolso.events.model.EventErrorType
import br.com.guiabolso.events.model.EventErrorType.Generic
import br.com.guiabolso.events.model.EventMessage
import br.com.guiabolso.events.server.EventProcessor
import br.com.guiabolso.events.server.exception.ExceptionHandlerRegistry
import br.com.guiabolso.events.server.handler.EventHandler
import br.com.guiabolso.events.server.handler.EventHandlerDiscovery
import br.com.guiabolso.events.server.handler.SimpleEventHandlerRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventProcessorConfiguration {

    @Bean
    fun partnerEventHandlerDiscovery(
        configService: ConfigService,
        handlers: List<EventHandler>,
        @Qualifier("partnerEventDispatcher") dispatcher: EventDispatcher
    ): EventHandlerDiscovery {
        val logEnabled = configService.getBoolean("event.log.startAndFinish", true)
        val registry = SimpleEventHandlerRegistry()

        for (handler in handlers) {
            if (logEnabled) {
                registry.add(LoggingEventHandlerWrapper(handler))
            } else {
                registry.add(handler)
            }
        }

        return CompositeEventHandlerDiscovery(
            listOf(
                registry,
                ProxyingEventHandlerDiscovery(configService, dispatcher, NoOpAuthenticationService)
            )
        )
    }

    @Bean
    fun userEventHandlerDiscovery(
        configService: ConfigService,
        dataPackageConfiguration: DataPackageConfiguration,
        dispatcher: CachedEventDispatcher,
        eventCacheService: EventCacheService,
        @Qualifier("eventAuthenticationService") eventAuthenticationService: AuthenticationService
    ): EventHandlerDiscovery {
        val logEnabled = configService.getBoolean("event.log.startAndFinish", true)
        val registry = SimpleEventHandlerRegistry()

        dataPackageConfiguration.dataPackages.forEach {
            val eventHandler = MergingEventHandler(it, dispatcher, eventAuthenticationService, eventCacheService)

            if (logEnabled) {
                registry.add(LoggingEventHandlerWrapper(eventHandler))
            } else {
                registry.add(eventHandler)
            }
        }

        return CompositeEventHandlerDiscovery(
            listOf(
                registry,
                ProxyingEventHandlerDiscovery(configService, dispatcher, eventAuthenticationService)
            )
        )
    }

    @Bean
    fun createEventExceptionHandlerRegistry() = ExceptionHandlerRegistry().apply {
        register(RedirectException::class.java) { ex, event, reporter ->
            reporter.notifyError(ex, true)
            redirectFor(event, ex.payload)
        }
        register(EventException::class.java) { ex, event, reporter ->
            reporter.notifyError(ex, false)
            val errorType = if (ex.type is EventErrorType.Unknown) Generic else ex.type

            errorFor(event, errorType, EventMessage(ex.code, ex.parameters))
        }
    }

    @Bean
    fun partnerEventProcessor(
        @Qualifier("partnerEventHandlerDiscovery") eventHandlerDiscovery: EventHandlerDiscovery,
        exceptionHandlerRegistry: ExceptionHandlerRegistry
    ): EventProcessor {
        return EventProcessor(eventHandlerDiscovery, exceptionHandlerRegistry, Tracer)
    }

    @Bean
    fun userEventProcessor(
        @Qualifier("userEventHandlerDiscovery") eventHandlerDiscovery: EventHandlerDiscovery,
        exceptionHandlerRegistry: ExceptionHandlerRegistry
    ): EventProcessor {
        return EventProcessor(eventHandlerDiscovery, exceptionHandlerRegistry, Tracer)
    }
}
