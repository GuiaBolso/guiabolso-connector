package br.com.guiabolso.connector

import br.com.guiabolso.events.server.EventProcessor
import com.nhaarman.mockito_kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import ro.pippo.controller.Controller
import ro.pippo.controller.ControllerApplication
import ro.pippo.core.Pippo

@Configuration
@ComponentScan(basePackages = ["br.com.guiabolso.connector.**"])
class WebTestConfiguration {

    @Bean
    fun pippo(controllers: List<Controller>): Pippo {
        val app = object : ControllerApplication() {
            override fun onInit() {
                controllers.forEach {
                    addControllers(it)
                }
            }
        }.apply {
            this.pippoSettings.overrideSetting("server.host", "0.0.0.0")
            this.pippoSettings.overrideSetting("server.port", PORT)
            this.pippoSettings.overrideSetting("jetty.minThreads", "100")
            this.pippoSettings.overrideSetting("jetty.maxThreads", "500")
            this.pippoSettings.overrideSetting("jetty.idleTimeout", "10000")
        }

        return Pippo(app).also { it.start() }
    }

    @Bean
    fun partnerEventProcessor(): EventProcessor = mock()

    @Bean
    fun userEventProcessor(): EventProcessor = mock()

    companion object {
        const val PORT = 13131
    }
}
