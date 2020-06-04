package br.com.guiabolso.connector

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@Configuration
@ComponentScan(basePackages = ["br.com.guiabolso.connector.**"])
class SpringTestConfiguration
