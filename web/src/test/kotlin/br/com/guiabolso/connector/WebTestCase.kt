package br.com.guiabolso.connector

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles("test")
@ExtendWith(value = [SpringExtension::class])
@ContextConfiguration(classes = [WebTestConfiguration::class])
abstract class WebTestCase
