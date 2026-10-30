package com.cuzorlabs.ashwanitest.ui.auth

import com.cuzorlabs.ashwanitest.domain.auth.*
import com.cuzorlabs.ashwanitest.domain.auth.port.SessionStore
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

class AuthenticationRoutes(private val signIn: SignIn, private val authenticate: AuthenticateSession, private val changePassword: ChangePassword, private val signOut: SignOut, private val sessions: SessionStore) {
    fun install(route: Route) = with(route) {
        get("/login") { call.routeHtml(loginPage()) }
        post("/login") { val form = call.receiveParameters(); when (val result = signIn.execute(form["email"].orEmpty(), form["password"].orEmpty().toCharArray(), call.request.local.remoteHost)) { is SignInResult.Failure -> call.routeHtml(loginPage(true), HttpStatusCode.Unauthorized); is SignInResult.Success -> { call.response.cookies.append(Cookie(SESSION_COOKIE, result.session.token.value, httpOnly = true, secure = true, extensions = mapOf("SameSite" to "Strict"), path = "/")); call.response.cookies.append(Cookie(CSRF_COOKIE, result.session.csrfToken, secure = true, extensions = mapOf("SameSite" to "Strict"), path = "/")); call.respondRedirect(if (result.session.user.mustChangePassword) "/password/change" else "/") } } }
        get("/") { val (_, user) = call.authenticated(authenticate) ?: return@get; if (user.mustChangePassword) call.respondRedirect("/password/change") else call.routeHtml(page("Home", "<h1>Protected home</h1><form method=\"post\" action=\"/logout\"><input type=\"hidden\" name=\"csrf\" value=\"${call.request.cookies[CSRF_COOKIE].orEmpty()}\"><button>Sign out</button></form>")) }
        get("/password/change") { call.authenticated(authenticate) ?: return@get; call.routeHtml(passwordPage(call.request.cookies[CSRF_COOKIE].orEmpty())) }
        post("/password/change") { val (token, user) = call.authenticated(authenticate) ?: return@post; if (!call.requireCsrf(token, sessions)) { call.unauthorized(); return@post }; val errors = changePassword.execute(user, call.receiveParameters()["password"].orEmpty().toCharArray()); if (errors.isEmpty()) call.respondRedirect("/") else call.routeHtml(passwordPage(call.request.cookies[CSRF_COOKIE].orEmpty(), errors), HttpStatusCode.BadRequest) }
        post("/logout") { val (token, user) = call.authenticated(authenticate) ?: return@post; if (!call.requireCsrf(token, sessions)) { call.unauthorized(); return@post }; signOut.execute(token, user); call.response.cookies.append(Cookie(SESSION_COOKIE, "", maxAge = 0, path = "/", httpOnly = true, secure = true)); call.respondRedirect("/login") }
    }
}
internal suspend fun ApplicationCall.routeHtml(body: String, status: HttpStatusCode = HttpStatusCode.OK) { response.headers.append(HttpHeaders.CacheControl, "no-store"); response.headers.append("Content-Security-Policy", "default-src 'self'; form-action 'self'; frame-ancestors 'none'"); response.headers.append("X-Frame-Options", "DENY"); response.headers.append("X-Content-Type-Options", "nosniff"); respondText(body, ContentType.Text.Html, status) }
