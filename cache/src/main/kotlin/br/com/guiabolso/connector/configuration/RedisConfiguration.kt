package br.com.guiabolso.connector.configuration

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.FstCodec
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Redis
@Configuration
@PropertySource("classpath:/conf/redis.properties")
class RedisConfiguration {

    @Bean
    fun redissonClient(configService: ConfigService): RedissonClient {
        return Redisson.create(Config().apply {
            val servers = useReplicatedServers()
            val redisCluster = configService.getRequiredString("redis.address").split(",").toTypedArray()
            servers.addNodeAddress(*redisCluster)
            codec = FstCodec()
        })
    }
}
