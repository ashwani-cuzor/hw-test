package com.cuzorlabs.ashwanitest.app

import com.cuzorlabs.ashwanitest.ui.installSecurityHeaders
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8080
private const val PORT_VARIABLE = "PORT"
private const val HOST = "0.0.0.0"

fun main() {
    val provisioning = requireNotNull(System.getProperty("auth.bootstrap.password")) { "Administrator provisioning material must be supplied by deployment." }.toCharArray()
    val component = AppComponent(AuthConfiguration("jdbc:sqlite:authentication.db", provisioning))
    val port = System.getenv(PORT_VARIABLE)?.toIntOrNull() ?: DEFAULT_PORT
    embeddedServer(Netty, port = port, host = HOST) { installSecurityHeaders(); routing { component.authenticationRoutes.install(this); component.adminRoutes.install(this) } }.start(wait = true)
}
