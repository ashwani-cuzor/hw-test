package com.cuzorlabs.ashwanitest.ui

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HelloRoutesTest {
    @Test
    fun `shows hello world`() {
        testApplication {
            routing {
                helloRoutes()
            }

            val response = client.get("/")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Hello world", response.bodyAsText())
        }
    }
}
