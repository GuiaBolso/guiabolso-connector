package br.com.guiabolso.connector.token

import br.com.guiabolso.connector.token.exception.ExpiredRefreshTokenException
import br.com.guiabolso.connector.token.exception.UnauthorizedUserException
import br.com.guiabolso.connector.user.UserAuthorizationService
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserAuthorizationServiceTest {

    private lateinit var accessTokenProvider: AccessTokenProvider
    private lateinit var service: UserAuthorizationService

    @BeforeEach
    fun setUp() {
        accessTokenProvider = mock()
        service = UserAuthorizationService(accessTokenProvider)
    }

    @Test
    fun `should return true for no error while fetching access token`() {
        val userId = "1"
        assertThat(service.isUserAuthorized(userId)).isTrue()
    }

    @Test
    fun `should return false for unauthorized user while fetching access token`() {
        val userId = "1"

        whenever(accessTokenProvider.getAccessTokenBy(userId)).thenThrow(
            UnauthorizedUserException()
        )

        assertThat(service.isUserAuthorized(userId)).isFalse()
    }

    @Test
    fun `should return false for refresh token expired while fetching access token`() {
        val userId = "1"
        whenever(accessTokenProvider.getAccessTokenBy(userId)).thenThrow(ExpiredRefreshTokenException::class.java)
        assertThat(service.isUserAuthorized(userId)).isFalse()
    }
}
