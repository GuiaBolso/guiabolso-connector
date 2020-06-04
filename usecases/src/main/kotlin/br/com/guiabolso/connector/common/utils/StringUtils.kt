package br.com.guiabolso.connector.common.utils

fun ByteArray.toUTF8String() = this.toString(Charsets.UTF_8)
