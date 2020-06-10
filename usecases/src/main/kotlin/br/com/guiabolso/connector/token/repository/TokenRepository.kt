package br.com.guiabolso.connector.token.repository

import br.com.guiabolso.connector.common.cryptography.EncryptedData

interface TokenRepository {

    fun findAccessTokenBy(userId: String): EncryptedData?

    fun findRefreshTokenBy(userId: String): EncryptedData?

    fun updateAccessToken(userId: String, accessToken: EncryptedData)

    fun putToken(userId: String, accessToken: EncryptedData, refreshToken: EncryptedData)
}
