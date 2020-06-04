package br.com.guiabolso.connector.common.time

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.springframework.stereotype.Service

@Service
class UTCDateTimeService : ZonedDateTimeProvider {

    override fun now() = ZonedDateTime.now(ZoneOffset.UTC)!!
}
