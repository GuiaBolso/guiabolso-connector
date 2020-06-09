package br.com.guiabolso.connector.persistence.token

import br.com.guiabolso.connector.AbstractSpringTestCase
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.token.repository.TokenRepository
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.impl.PublicClaims
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.Date
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class StubbedTokenRepositoryTest : AbstractSpringTestCase() {

    @Autowired
    private lateinit var repository: TokenRepository

    @Test
    fun `should find access token`() {
        val userId = userId()
        val refreshToken = encryptedToken()
        val accessToken = encryptedToken()
        repository.insertToken(userId = userId, refreshToken = refreshToken, accessToken = accessToken)

        val actual = repository.findAccessTokenBy(userId)
        assertThat(actual).isEqualTo(accessToken)
    }

    @Test
    fun `should not find access token`() {
        val actual = repository.findAccessTokenBy(userId = "99")

        assertThat(actual).isNull()
    }

    @Test
    fun `should find refresh token`() {
        val userId = userId()
        val refreshToken = encryptedToken()
        val accessToken = encryptedToken()
        repository.insertToken(userId = userId, refreshToken = refreshToken, accessToken = accessToken)

        val actual = repository.findRefreshTokenBy(userId)

        assertThat(actual).isEqualTo(refreshToken)
    }

    @Test
    fun `should not find refresh token`() {
        val actual = repository.findRefreshTokenBy(userId = "99")

        assertThat(actual).isNull()
    }

    @Test
    fun `should update access token`() {
        val userId = userId()
        repository.insertToken(userId = userId, refreshToken = encryptedToken(), accessToken = encryptedToken())

        val accessToken = encryptedToken()
        repository.updateAccessToken(userId, accessToken)

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(accessToken)
    }

    @Test
    fun `should insert token`() {
        val userId = "4"
        val accessToken = encryptedToken()
        val refreshToken = encryptedToken()

        repository.insertToken(userId, accessToken, refreshToken)

        val actual = Pair(
            first = repository.findAccessTokenBy(userId),
            second = repository.findRefreshTokenBy(userId)
        )

        assertThat(actual.first).isEqualTo(accessToken)
        assertThat(actual.second).isEqualTo(refreshToken)
    }

    @Test
    fun `entry should be unique by userId`() {
        val userId = userId()
        repository.insertToken(userId = userId, refreshToken = encryptedToken(), accessToken = encryptedToken())

        val refreshToken = encryptedToken()
        val accessToken = encryptedToken()
        repository.insertToken(userId = userId, refreshToken = refreshToken, accessToken = accessToken)

        val currentRefreshToken = repository.findRefreshTokenBy(userId)
        val currentAccessToken = repository.findAccessTokenBy(userId)

        assertThat(currentAccessToken).isEqualTo(accessToken)
        assertThat(currentRefreshToken).isEqualTo(refreshToken)
    }

    private fun userId() = UUID.randomUUID().toString()

    private fun encryptedToken() = EncryptedData("encrypted.${jwt()}".toByteArray())

    private fun jwt(): String {
        return JWT.create()
            .withClaim(PublicClaims.ISSUED_AT, now().toDate())
            .withClaim(PublicClaims.JWT_ID, UUID.randomUUID().toString())
            .withClaim(PublicClaims.EXPIRES_AT, now().plusDays(1).toDate())
            .sign(Algorithm.none())
    }

    private fun ZonedDateTime.toDate() = Date.from(this.toInstant())
}
