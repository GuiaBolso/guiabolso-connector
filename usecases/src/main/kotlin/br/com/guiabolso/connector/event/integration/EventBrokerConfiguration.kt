package br.com.guiabolso.connector.event.integration

import br.com.guiabolso.events.client.EventClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventBrokerConfiguration {

    @Bean
    fun eventClient() = EventClient()
}
