repositories {
    maven {
        url = uri("https://s3-sa-east-1.amazonaws.com/dynamodb-local-sao-paulo/release")
    }
}

extra.set("aws-sdk-version", "1.11.792")
dependencies {
    implementation(aws("dynamodb"))
    implementation(aws("kms"))
    implementation(aws("s3"))
    implementation("com.amazonaws:aws-encryption-sdk-java:1.6.2")

    implementation(project(":usecases"))

    testImplementation("com.amazonaws:DynamoDBLocal:1.12.0")
    testImplementation("io.findify:s3mock_2.12:0.2.6")
}

@Suppress("unused")
fun DependencyHandler.aws(module: String): Any =
    "com.amazonaws:aws-java-sdk-$module:${extra.get("aws-sdk-version")}"

// workaround for testing Dynamo DB, proposed at: https://stackoverflow.com/a/36777446
tasks.register("copyNativeDeps", Copy::class.java) {
    from(configurations.runtimeClasspath.get() + configurations.testRuntimeClasspath.get()) {
        include("*.dll", "*.dylib", "*.so")
    }.into("build/libs")
}

tasks.test {
    dependsOn("copyNativeDeps")
    systemProperties(Pair("java.library.path", "build/libs"))
}
