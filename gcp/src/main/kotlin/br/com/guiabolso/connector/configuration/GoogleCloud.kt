package br.com.guiabolso.connector.configuration

import org.springframework.context.annotation.Profile

@Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
@Retention
@Profile("gcp & production")
annotation class GoogleCloud
