dependencies {
    implementation(pippo("pippo-core"))
    implementation(pippo("pippo-controller"))
    implementation(pippo("pippo-jetty"))
    implementation(pippo("pippo-gson"))

    implementation(project(":usecases"))
    implementation(project(":events"))
}

@Suppress("unused")
fun DependencyHandler.pippo(module: String): Any =
    "ro.pippo:$module:${rootProject.extra.get("pippo-version")}"
