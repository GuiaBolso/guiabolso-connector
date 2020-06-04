plugins {
    kotlin("jvm") version "1.3.70"
    kotlin("plugin.allopen") version "1.3.70"
    jacoco
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

buildscript {
    extra.apply {
        set("spring-version", "5.1.9.RELEASE")
        set("events-version", "2.10.0")
        set("pippo-version", "1.13.1")
        set("datadog-version", "0.49.0")
        set("open-tracing-version", "0.32.0")
    }
}

repositories {
    jcenter()
    mavenCentral()
}

subprojects {
    group = "br.com.guiabolso"
    version = "1.0.0"

    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring")
    apply(plugin = "jacoco")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))
        implementation(spring("spring-context"))

        implementation("ch.qos.logback:logback-classic:1.2.3")

        implementation(eventsProtocol("tracing"))
        implementation(eventsProtocol("events-core"))
        implementation(eventsProtocol("events-server"))

        implementation("io.opentracing:opentracing-api:${rootProject.extra.get("open-tracing-version")}")
        implementation("io.opentracing:opentracing-util:${rootProject.extra.get("open-tracing-version")}")
        implementation("com.datadoghq:dd-trace-ot:${rootProject.extra.get("datadog-version")}")
        implementation("com.datadoghq:dd-trace-api:${rootProject.extra.get("datadog-version")}")

        testImplementation("org.mockito:mockito-core:2.21.0")
        testImplementation("com.nhaarman:mockito-kotlin:1.6.0")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")
        testImplementation("org.assertj:assertj-core:3.16.1")
        testImplementation(spring("spring-test"))
        testImplementation("org.jeasy:easy-random-core:4.2.0")
        testImplementation("io.rest-assured:rest-assured:4.3.0")
        testImplementation("org.mockito:mockito-inline:3.3.3")
    }

    tasks.compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjvm-default=enable")
            allWarningsAsErrors = false
            jvmTarget = "1.8"
        }
    }

    jacoco {
        toolVersion = "0.8.3"
        reportsDir = file("$buildDir/reports/jacoco")
    }

    ktlint {
        version.set("0.34.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.jacocoTestReport {
        reports {
            xml.isEnabled = true
            csv.isEnabled = false
        }
    }
}

@Suppress("unused")
fun DependencyHandler.spring(module: String): Any =
    "org.springframework:$module:${rootProject.extra.get("spring-version")}"

@Suppress("unused")
fun DependencyHandler.eventsProtocol(module: String): Any =
    "br.com.guiabolso:$module:${rootProject.extra.get("events-version")}"
