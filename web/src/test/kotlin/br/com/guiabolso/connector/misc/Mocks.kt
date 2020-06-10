package br.com.guiabolso.connector.misc

import br.com.guiabolso.connector.common.cryptography.CryptographyService
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.token.repository.TokenRepository
import org.springframework.stereotype.Service

@Service("configDecryptorService")
class MockedCryptographyService : CryptographyService {
    override fun encrypt(data: ByteArray) = EncryptedData(data)
    override fun decrypt(encryptedData: EncryptedData) = encryptedData.value
}

@Service
class MockedTokenRepository : TokenRepository {
    override fun findAccessTokenBy(userId: String): EncryptedData? = null
    override fun findRefreshTokenBy(userId: String): EncryptedData? = null
    override fun updateAccessToken(userId: String, accessToken: EncryptedData) {}
    override fun putToken(userId: String, accessToken: EncryptedData, refreshToken: EncryptedData) {}
}
