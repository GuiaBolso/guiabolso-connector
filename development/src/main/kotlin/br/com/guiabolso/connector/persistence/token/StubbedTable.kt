package br.com.guiabolso.connector.persistence.token

sealed class StubbedTable {

    data class Token(
        val userId: String,
        var accessToken: String,
        var refreshToken: String
    ) : StubbedTable()
}
