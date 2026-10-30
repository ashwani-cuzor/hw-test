package com.cuzorlabs.ashwanitest.ui

import io.ktor.server.application.Application

/** Headers are added by route handlers so pages are never cacheable after authentication. */
fun Application.installSecurityHeaders() = Unit
