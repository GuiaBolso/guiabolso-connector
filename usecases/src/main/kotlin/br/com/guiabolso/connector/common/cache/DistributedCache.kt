package br.com.guiabolso.connector.common.cache

import br.com.guiabolso.connector.common.cryptography.EncryptedData
import java.time.Duration

interface DistributedCache {

    fun getData(key: String, duration: Duration): EncryptedData?

    fun putData(key: String, value: EncryptedData, duration: Duration)

    fun invalidateData(key: String)
}
