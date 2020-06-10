package br.com.guiabolso.connector.persistence.token

import br.com.guiabolso.connector.GcpErrorCode.DATASTORE_FAILURE
import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.configuration.ConfigService
import br.com.guiabolso.connector.configuration.GoogleCloud
import br.com.guiabolso.connector.event.exception.PersistenceException
import br.com.guiabolso.connector.token.repository.TokenRepository
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreException
import com.google.cloud.datastore.Entity
import org.springframework.stereotype.Repository

@GoogleCloud
@Repository
class DatastoreTokenRepository(
    configService: ConfigService,
    private val datastore: Datastore
) : TokenRepository {

    private val keyFactory = datastore.newKeyFactory().setKind(configService.getRequiredString("datastore.kind.token"))

    override fun findAccessTokenBy(userId: String): EncryptedData? {
        return findTokenBy(userId)?.let {
            EncryptedData(it.getString(ACCESS_TOKEN).toByteArray())
        }
    }

    override fun findRefreshTokenBy(userId: String): EncryptedData? {
        return findTokenBy(userId)?.let {
            EncryptedData(it.getString(REFRESH_TOKEN).toByteArray())
        }
    }

    override fun updateAccessToken(userId: String, accessToken: EncryptedData) {
        val tokenEntity = findTokenBy(userId) ?: throw PersistenceException(
            DATASTORE_FAILURE,
            "Could not update data into Datastore because it doesn't exists"
        )

        val updatedTokenEntity = Entity.newBuilder(tokenEntity)
            .set(ACCESS_TOKEN, accessToken.stringValue())
            .build()

        try {
            datastore.put(updatedTokenEntity)
        } catch (e: DatastoreException) {
            throw PersistenceException(DATASTORE_FAILURE, "Could not update data into Datastore", e)
        }
    }

    override fun putToken(userId: String, accessToken: EncryptedData, refreshToken: EncryptedData) {
        val entity = Entity.newBuilder(keyFactory.newKey(userId))
            .set(ACCESS_TOKEN, accessToken.stringValue())
            .set(REFRESH_TOKEN, refreshToken.stringValue())
            .build()

        try {
            datastore.put(entity)
        } catch (e: DatastoreException) {
            throw PersistenceException(DATASTORE_FAILURE, "Could not insert data into Datastore", e)
        }
    }

    private fun findTokenBy(userId: String): Entity? {
        return try {
            datastore.get(keyFactory.newKey(userId))
        } catch (e: DatastoreException) {
            throw PersistenceException(DATASTORE_FAILURE, "Could not fetch data into Datastore", e)
        }
    }

    companion object {
        private const val ACCESS_TOKEN = "accessToken"
        private const val REFRESH_TOKEN = "refreshToken"
    }
}
