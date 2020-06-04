package br.com.guiabolso.connector.token.model

class Token {

    companion object {
        @JvmStatic
        fun getKey(userId: String) = "${Token::class.java.simpleName}.$userId"
    }
}
