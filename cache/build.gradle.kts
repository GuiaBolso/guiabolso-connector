dependencies {
    implementation("org.redisson:redisson:3.13.0")
    implementation("de.ruedigermoeller:fst:2.57")

    testImplementation("it.ozimov:embedded-redis:0.7.2")
    testImplementation("javax.annotation:javax.annotation-api:1.3.2")

    implementation(project(":usecases"))
}
