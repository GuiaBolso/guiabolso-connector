dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.3")

    implementation("com.auth0:java-jwt:3.10.3")

    implementation("br.com.guiabolso:events-client:${rootProject.extra.get("events-version")}")
}
