package br.com.guiabolso.connector.user

import br.com.guiabolso.connector.token.AccessTokenProvider
import br.com.guiabolso.connector.token.exception.ExpiredRefreshTokenException
import br.com.guiabolso.connector.token.exception.UnauthorizedUserException
import org.springframework.stereotype.Service

@Service
class UserAuthorizationService(
    private val accessTokenProvider: AccessTokenProvider
) {

    fun isUserAuthorized(userId: String): Boolean {
        return try {
            accessTokenProvider.getAccessTokenBy(userId)
            true
        } catch (e: RuntimeException) {
            when (e) {
                is UnauthorizedUserException,
                is ExpiredRefreshTokenException -> false
                else -> throw e
            }
        }
    }
}
