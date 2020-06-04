package br.com.guiabolso.connector.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

object LoggerConfigurer {

    fun setupLogger(logLevel: String) {
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

        val configuredLevel = when (logLevel.toUpperCase()) {
            "INFO" -> Level.INFO
            "DEBUG" -> Level.DEBUG
            "WARN" -> Level.WARN
            "ERROR" -> Level.ERROR
            else -> Level.INFO
        }

        if (configuredLevel == rootLogger.level) return

        rootLogger.info("Override root log level ${rootLogger.level} with $configuredLevel")
        rootLogger.level = configuredLevel
    }
}
