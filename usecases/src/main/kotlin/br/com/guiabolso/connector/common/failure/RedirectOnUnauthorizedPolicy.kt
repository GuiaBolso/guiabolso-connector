package br.com.guiabolso.connector.common.failure

enum class RedirectOnUnauthorizedPolicy {
    NEVER,
    ALWAYS,
    USER_EVENTS,
    PARTNER_EVENTS;
}
