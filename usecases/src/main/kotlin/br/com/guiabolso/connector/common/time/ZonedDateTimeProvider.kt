package br.com.guiabolso.connector.common.time

import java.time.ZonedDateTime

interface ZonedDateTimeProvider {

    fun now(): ZonedDateTime
}
