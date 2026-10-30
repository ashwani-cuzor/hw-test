package com.cuzorlabs.ashwanitest.app

import com.cuzorlabs.ashwanitest.ui.helloRoutes
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8080
private const val PORT_VARIABLE = "PORT"
private const val HOST = "0.0.0.0"

fun main() {
    val port = System.getenv(PORT_VARIABLE)?.toIntOrNull() ?: DEFAULT_PORT
    embeddedServer(Netty, port = port, host = HOST) {
        routing {
            helloRoutes()
        }
    }.start(wait = true)
}
