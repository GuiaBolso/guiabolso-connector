package br.com.guiabolso.connector.persistence.token

import br.com.guiabolso.connector.AbstractSpringTestCase
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.token.repository.TokenRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class StubbedTokenRepositoryTest : AbstractSpringTestCase() {

    @Autowired
    private lateinit var repository: TokenRepository

    @Test
    fun `should find access token`() {
        val userId = "1"
        val expected = EncryptedData("encrypted.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw".toByteArray())

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should not find access token`() {
        val actual = repository.findAccessTokenBy(userId = "99")

        assertThat(actual).isNull()
    }

    @Test
    fun `should find refresh token`() {
        val userId = "2"
        val expected = EncryptedData("encrypted.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZwx".toByteArray())

        val actual = repository.findRefreshTokenBy(userId)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should not find refresh token`() {
        val actual = repository.findRefreshTokenBy(userId = "99")

        assertThat(actual).isNull()
    }

    @Test
    fun `should update access token`() {
        val userId = "1"
        val accessToken =
            EncryptedData("encrypted.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw".toByteArray())

        repository.updateAccessToken(userId, accessToken)

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(accessToken)
    }

    @Test
    fun `should insert token`() {
        val userId = "4"
        val accessToken =
            EncryptedData("encrypted.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw".toByteArray())
        val refreshToken =
            EncryptedData("encrypted.eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjQxMjYyNjU3NzZ9.CaoYFrYIoRzEU1Pmx2YzsVVXfhjcXSUQpoGTisbLXZw".toByteArray())

        repository.insertToken(userId, accessToken, refreshToken)

        val actual = Pair(
            first = repository.findAccessTokenBy(userId),
            second = repository.findRefreshTokenBy(userId)
        )

        assertThat(actual.first).isEqualTo(accessToken)
        assertThat(actual.second).isEqualTo(refreshToken)
    }
}
