package br.com.guiabolso.connector.common.profile

import org.springframework.context.annotation.Profile

@Target(allowedTargets = [AnnotationTarget.CLASS, AnnotationTarget.FUNCTION])
@Retention
@Profile("!production")
annotation class Development
