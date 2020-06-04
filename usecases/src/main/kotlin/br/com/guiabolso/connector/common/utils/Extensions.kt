package br.com.guiabolso.connector.common.utils

import java.nio.ByteBuffer
import java.util.Base64

fun ByteArray.encodeBase64() = Base64.getEncoder().encode(this)!!

fun ByteArray.decodeBase64() = Base64.getDecoder().decode(this)!!

fun ByteArray.toByteBuffer(): ByteBuffer {
    val byteBuffer = ByteBuffer.allocate(this.size)
    byteBuffer.put(this)
    byteBuffer.flip()
    return byteBuffer
}
