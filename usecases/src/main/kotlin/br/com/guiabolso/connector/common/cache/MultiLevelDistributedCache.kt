package br.com.guiabolso.connector.common.cache

interface MultiLevelDistributedCache : DistributedCache {

    val level: Int
}
