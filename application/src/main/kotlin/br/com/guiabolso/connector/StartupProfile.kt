package br.com.guiabolso.connector

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class StartupProfile(environment: Environment) {
    private val activeProfiles = environment.activeProfiles

    init {
        if ("production" in activeProfiles) info() else warn()
    }

    private fun info() {
        logger.info("Application started in production mode with profiles [${activeProfiles.joinToString()}] active!")
    }

    private fun warn() {
        logger.warn("##########################################")
        logger.warn("#### Application in development mode! ####")
        logger.warn("##########################################")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(StartupProfile::class.java)
    }
}
