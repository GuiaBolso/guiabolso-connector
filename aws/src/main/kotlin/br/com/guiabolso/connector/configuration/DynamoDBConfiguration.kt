package br.com.guiabolso.connector.configuration

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Table
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Aws
@Configuration
class DynamoDBConfiguration(configService: ConfigService) {

    private val region = configService.getRequiredString("dynamodb.region")
    private val table = configService.getRequiredString("dynamodb.table")

    @Bean
    fun dynamoDBClient(): AmazonDynamoDB {
        return AmazonDynamoDBClientBuilder
            .standard()
            .withRegion(region)
            .build()
    }

    @Bean
    fun dynamoDBTable(client: AmazonDynamoDB): Table {
        return DynamoDB(client).getTable(table)
    }
}
