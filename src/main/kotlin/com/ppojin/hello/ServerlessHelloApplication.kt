package com.ppojin.hello

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class ServerlessHelloApplication

fun main(args: Array<String>) {
    runApplication<ServerlessHelloApplication>(*args)
}

