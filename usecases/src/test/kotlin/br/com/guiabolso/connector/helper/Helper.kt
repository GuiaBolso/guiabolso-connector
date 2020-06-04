package br.com.guiabolso.connector.helper

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.impl.PublicClaims
import java.time.ZonedDateTime
import java.util.Date

fun createJWT(issuedAt: ZonedDateTime, expireAt: ZonedDateTime): String {
    return JWT.create()
        .withClaim(PublicClaims.ISSUED_AT, issuedAt.toDate())
        .withClaim(PublicClaims.EXPIRES_AT, expireAt.toDate())
        .sign(Algorithm.none())
}

fun ZonedDateTime.toDate(): Date = Date.from(this.toInstant())
