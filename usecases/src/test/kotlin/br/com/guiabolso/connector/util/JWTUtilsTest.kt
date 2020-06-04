package br.com.guiabolso.connector.util

import br.com.guiabolso.connector.common.utils.isExpired
import br.com.guiabolso.connector.helper.createJWT
import com.auth0.jwt.JWT
import java.time.ZonedDateTime.now
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JWTUtilsTest {

    @Test
    fun `should tell whether a token is expired when it is`() {
        val now = now()
        val expiredOneSecondsAgo = now.minusSeconds(1)

        val jwt = createJWT(issuedAt = now.minusHours(1), expireAt = expiredOneSecondsAgo)
        val decode = JWT.decode(jwt)

        assertTrue(decode.isExpired(now))
    }

    @Test
    fun `should tell whether a token is expired when it's not`() {
        val now = now()
        val expireAt = now.plusSeconds(1)

        val jwt = createJWT(issuedAt = now, expireAt = expireAt)
        val decoded = JWT.decode(jwt)

        assertFalse(decoded.isExpired(now))
    }
}
