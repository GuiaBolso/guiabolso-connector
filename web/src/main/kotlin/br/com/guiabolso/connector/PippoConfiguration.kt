package br.com.guiabolso.connector

import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.logger.LoggerConfigurer
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ro.pippo.controller.Controller
import ro.pippo.controller.ControllerApplication
import ro.pippo.core.Pippo
import ro.pippo.core.PippoSettings
import ro.pippo.core.RuntimeMode

@Configuration
class PippoConfiguration {

    @Bean
    fun startPippo(beanFactory: ListableBeanFactory, configService: ConfigService): Pippo {
        configureLogger(configService)

        val controllers = beanFactory.getBeansOfType(Controller::class.java).values

        val settings = PippoSettings(RuntimeMode.getCurrent())

        injectRequiredConfig(configService, settings, "application.name")
        injectRequiredConfig(configService, settings, "application.version")
        injectRequiredConfig(configService, settings, "application.languages")
        injectRequiredConfig(configService, settings, "application.name")

        injectRequiredConfig(configService, settings, "server.port")
        injectRequiredConfig(configService, settings, "server.host")
        injectRequiredConfig(configService, settings, "server.contextPath")

        injectRequiredConfig(configService, settings, "jetty.minThreads")
        injectRequiredConfig(configService, settings, "jetty.maxThreads")
        injectRequiredConfig(configService, settings, "jetty.idleTimeout")

        injectRequiredConfig(configService, settings, "metrics.mbeans.enabled")

        val app = object : ControllerApplication(settings) {
            override fun onInit() {
                router.ignorePaths("/favicon.ico")
                controllers.forEach { addControllers(it) }
            }
        }

        val pippo = Pippo(app)
        pippo.start()

        return pippo
    }

    private fun injectRequiredConfig(configService: ConfigService, settings: PippoSettings, property: String) {
        settings.overrideSetting(property, configService.getRequiredString(property))
    }

    private fun configureLogger(configService: ConfigService) {
        val logLevel = configService.getString("root.logger.level", "INFO")
        LoggerConfigurer.setupLogger(logLevel)
    }
}
