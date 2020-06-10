package br.com.guiabolso.connector.persistence.token

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.configuration.Aws
import br.com.guiabolso.connector.token.repository.TokenRepository
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import org.springframework.stereotype.Repository

@Aws
@Repository
class DynamoDBTokenRepository(
    private val table: Table
) : TokenRepository {

    override fun findAccessTokenBy(userId: String): EncryptedData? {
        val spec = QuerySpec()
            .withKeyConditionExpression("userId = :userId")
            .withValueMap(ValueMap().withString(":userId", userId))
            .withMaxResultSize(1)

        return table.query(spec)
            .firstOrNull()
            ?.getString("accessToken")
            ?.let { EncryptedData(it.toByteArray()) }
    }

    override fun findRefreshTokenBy(userId: String): EncryptedData? {
        val spec = QuerySpec()
            .withKeyConditionExpression("userId = :userId")
            .withValueMap(ValueMap().withString(":userId", userId))
            .withMaxResultSize(1)

        return table.query(spec)
            .firstOrNull()
            ?.getString("refreshToken")
            ?.let { EncryptedData(it.toByteArray()) }
    }

    override fun updateAccessToken(userId: String, accessToken: EncryptedData) {
        val spec = UpdateItemSpec()
            .withPrimaryKey("userId", userId)
            .withAttributeUpdate(
                AttributeUpdate("accessToken").put(accessToken.stringValue())
            )

        table.updateItem(spec)
    }

    override fun putToken(userId: String, accessToken: EncryptedData, refreshToken: EncryptedData) {
        val item = Item().withPrimaryKey("userId", userId)
            .withString("accessToken", accessToken.stringValue())
            .withString("refreshToken", refreshToken.stringValue())

        table.putItem(item)
    }
}
