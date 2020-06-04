package br.com.guiabolso.connector

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["br.com.guiabolso.connector.**"])
class Boot {

    companion object {
        @JvmStatic
        @Suppress("UnusedMainParameter")
        fun main(vararg args: String) {
            AnnotationConfigApplicationContext(Boot::class.java)
        }
    }
}
