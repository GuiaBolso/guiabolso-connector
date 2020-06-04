package br.com.guiabolso.connector.event.misc

import br.com.guiabolso.connector.common.credentials.ClientCredentials
import br.com.guiabolso.events.json.MapperHolder.mapper
import br.com.guiabolso.events.model.RequestEvent

fun RequestEvent.authenticatedAsUser(clientCredentials: ClientCredentials, accessToken: String) = copy(
    identity = mapper.toJsonTree(
        mapOf(
            "userId" to identity["userId"].asString,
            "clientId" to clientCredentials.clientId
        )
    ).asJsonObject,
    auth = mapper.toJsonTree(
        mapOf(
            "clientSecret" to clientCredentials.clientSecret,
            "accessToken" to accessToken
        )
    ).asJsonObject
)

fun RequestEvent.authenticatedAsClient(clientCredentials: ClientCredentials) = copy(
    identity = mapper.toJsonTree(
        mapOf(
            "clientId" to clientCredentials.clientId
        )
    ).asJsonObject,
    auth = mapper.toJsonTree(
        mapOf(
            "clientSecret" to clientCredentials.clientSecret
        )
    ).asJsonObject
)
