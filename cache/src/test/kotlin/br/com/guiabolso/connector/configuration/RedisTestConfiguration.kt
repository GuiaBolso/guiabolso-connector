package br.com.guiabolso.connector.configuration

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.FstCodec
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.embedded.RedisServer

@Configuration
class RedisTestConfiguration {

    private lateinit var server: RedisServer

    @PostConstruct
    fun init() {
        server = RedisServer.builder()
            .port(PORT)
            .setting("appendonly no")
            .setting("maxmemory 128M")
            .build()

        server.start()
    }

    @PreDestroy
    fun destroy() {
        if (server.isActive) server.stop()
    }

    @Bean
    fun redissonClient(): RedissonClient {
        return Redisson.create(Config().apply {
            val servers = useReplicatedServers()
            val redisCluster = "redis://$HOSTNAME:$PORT"
            servers.addNodeAddress(redisCluster)
            codec = FstCodec()
        })
    }

    companion object {
        private const val HOSTNAME = "127.0.0.1"
        private const val PORT = 6379
    }
}
