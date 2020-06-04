package br.com.guiabolso.connector.common.utils

import com.auth0.jwt.interfaces.DecodedJWT
import java.time.ZonedDateTime

fun DecodedJWT.isExpired(reference: ZonedDateTime): Boolean {
    return this.expiresAt.toInstant().isBefore(reference.toInstant())
}
