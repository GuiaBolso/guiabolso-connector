package br.com.guiabolso.connector.common.credentials

interface ClientCredentialsProvider {

    fun clientCredentials(): ClientCredentials
}
