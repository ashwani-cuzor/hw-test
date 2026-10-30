package com.cuzorlabs.ashwanitest.ui

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.helloRoutes() {
    get("/") {
        call.respondText("Hello world")
    }
}
