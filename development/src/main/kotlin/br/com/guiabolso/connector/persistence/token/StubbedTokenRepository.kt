package br.com.guiabolso.connector.persistence.token

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.common.profile.Development
import br.com.guiabolso.connector.configuration.InMemoryDatabase
import br.com.guiabolso.connector.configuration.findAllByType
import br.com.guiabolso.connector.persistence.token.StubbedTable.Token
import br.com.guiabolso.connector.token.repository.TokenRepository
import org.springframework.stereotype.Repository

@Development
@Repository
class StubbedTokenRepository(private val database: InMemoryDatabase) : TokenRepository {

    override fun findAccessTokenBy(userId: String): EncryptedData? {
        return database.findAllByType<Token>()
            .firstOrNull { it.userId == userId }
            ?.accessToken
            ?.let { EncryptedData(it.toByteArray()) }
    }

    override fun findRefreshTokenBy(userId: String): EncryptedData? {
        return database.findAllByType<Token>()
            .firstOrNull { it.userId == userId }
            ?.refreshToken
            ?.let { EncryptedData(it.toByteArray()) }
    }

    override fun updateAccessToken(userId: String, accessToken: EncryptedData) {
        database.findAllByType<Token>()
            .firstOrNull { it.userId == userId }
            ?.accessToken = accessToken.stringValue()
    }

    override fun insertToken(userId: String, accessToken: EncryptedData, refreshToken: EncryptedData) {
        database.findAllByType<Token>()
            .add(
                Token(
                    userId = userId,
                    accessToken = accessToken.stringValue(),
                    refreshToken = refreshToken.stringValue()
                )
            )
    }
}
