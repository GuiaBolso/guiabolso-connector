package br.com.guiabolso.connector.persistence.token

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest
import com.amazonaws.services.dynamodbv2.model.CreateTableResult
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement
import com.amazonaws.services.dynamodbv2.model.KeyType
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import java.util.Collections
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DynamoDBTokenRepositoryTest {

    private lateinit var client: AmazonDynamoDB
    private lateinit var document: DynamoDB
    private lateinit var table: Table
    private lateinit var repository: DynamoDBTokenRepository

    @BeforeEach
    fun setUp() {
        client = DynamoDBEmbedded.create().amazonDynamoDB()
        document = DynamoDB(client)
        table = document.getTable(TABLE_NAME)
        repository = DynamoDBTokenRepository(table)

        createTable(client)
    }

    @AfterEach
    fun tearDown() {
        client.shutdown()
    }

    @Test
    fun `should find access token`() {
        val userId = nextObject<String>()
        val accessToken = EncryptedData(nextObject<String>().toByteArray())

        val item = Item()
            .withPrimaryKey("userId", userId)
            .withString("accessToken", accessToken.stringValue())

        table.putItem(item)

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(accessToken)
    }

    @Test
    fun `should not find access token`() {
        val actual = repository.findAccessTokenBy(userId = "1")

        assertThat(actual).isNull()
    }

    @Test
    fun `should find refresh token`() {
        val userId = nextObject<String>()
        val refreshToken = EncryptedData(nextObject<String>().toByteArray())

        val item = Item()
            .withPrimaryKey("userId", userId)
            .withString("refreshToken", refreshToken.stringValue())

        table.putItem(item)

        val actual = repository.findRefreshTokenBy(userId)

        assertThat(actual).isEqualTo(refreshToken)
    }

    @Test
    fun `should not find refresh token`() {
        val actual = repository.findRefreshTokenBy(userId = "1")

        assertThat(actual).isNull()
    }

    @Test
    fun `should update access token`() {
        val userId = nextObject<String>()
        val accessToken = EncryptedData(nextObject<String>().toByteArray())

        val item = Item()
            .withPrimaryKey("userId", userId)
            .withString("accessToken", accessToken.stringValue())

        table.putItem(item)

        repository.updateAccessToken(userId, accessToken)

        val actual = repository.findAccessTokenBy(userId)

        assertThat(actual).isEqualTo(accessToken)
    }

    @Test
    fun `should insert token`() {
        val userId = nextObject<String>()
        val accessToken = EncryptedData(nextObject<String>().toByteArray())
        val refreshToken = EncryptedData(nextObject<String>().toByteArray())

        repository.insertToken(userId, accessToken, refreshToken)

        val actual = Pair(
            first = repository.findAccessTokenBy(userId),
            second = repository.findRefreshTokenBy(userId)
        )

        assertThat(actual.first).isEqualTo(accessToken)
        assertThat(actual.second).isEqualTo(refreshToken)
    }

    private fun createTable(client: AmazonDynamoDB): CreateTableResult {
        val userIdAttr = AttributeDefinition()
            .withAttributeName("userId")
            .withAttributeType(ScalarAttributeType.S)

        val partitionKeyType = KeyType.HASH

        val userIdKeyElem = KeySchemaElement()
            .withAttributeName("userId")
            .withKeyType(partitionKeyType)

        val request = CreateTableRequest()
            .withTableName(TABLE_NAME)
            .withAttributeDefinitions(Collections.singletonList(userIdAttr))
            .withKeySchema(Collections.singletonList(userIdKeyElem))
            .withProvisionedThroughput(ProvisionedThroughput(5, 5))

        return client.createTable(request)
    }

    companion object {
        private const val TABLE_NAME = "gbconnect-db"
    }
}
