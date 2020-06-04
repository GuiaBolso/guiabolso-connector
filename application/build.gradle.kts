plugins {
    application
}

application {
    mainClassName = "br.com.guiabolso.connector.Boot"

    applicationDefaultJvmArgs = listOf(
        "-server",
        "-XX:+UseNUMA",
        "-XX:+UseG1GC",
        "-Duser.timezone=America/Sao_Paulo",
        "-Dlogback.configurationFile=logback-production.xml"
    )
}

dependencies {
    implementation(project(":cache"))
    implementation(project(":usecases"))
    implementation(project(":events"))
    implementation(project(":web"))
    implementation(project(":aws"))
    implementation(project(":gcp"))
    implementation(project(":development"))
}
