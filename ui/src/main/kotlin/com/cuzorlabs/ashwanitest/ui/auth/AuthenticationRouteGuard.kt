package com.cuzorlabs.ashwanitest.ui.auth

import com.cuzorlabs.ashwanitest.domain.auth.AuthenticateSession
import com.cuzorlabs.ashwanitest.domain.auth.model.AuthenticatedUser
import com.cuzorlabs.ashwanitest.domain.auth.model.SessionToken
import com.cuzorlabs.ashwanitest.domain.auth.port.SessionStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText

const val SESSION_COOKIE = "auth_session"
const val CSRF_COOKIE = "auth_csrf"
suspend fun ApplicationCall.authenticated(authenticate: AuthenticateSession): Pair<String, AuthenticatedUser>? { val token = request.cookies[SESSION_COOKIE] ?: run { respondRedirect("/login"); return null }; val user = authenticate.execute(token) ?: run { respondRedirect("/login"); return null }; return token to user }
suspend fun ApplicationCall.requireCsrf(token: String, sessions: SessionStore): Boolean = sessions.csrfMatches(SessionToken(token), receiveParameters()["csrf"].orEmpty())
suspend fun ApplicationCall.forbidden() { respondText("Forbidden", status = HttpStatusCode.Forbidden) }
suspend fun ApplicationCall.unauthorized() { respondText("Unauthorized", status = HttpStatusCode.Unauthorized) }
