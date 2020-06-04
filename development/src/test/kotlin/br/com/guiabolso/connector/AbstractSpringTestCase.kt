package br.com.guiabolso.connector

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@ActiveProfiles("test")
@ExtendWith(value = [SpringExtension::class])
@ContextConfiguration(classes = [SpringTestConfiguration::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
abstract class AbstractSpringTestCase
