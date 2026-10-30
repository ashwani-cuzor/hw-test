package com.cuzorlabs.ashwanitest.ui.auth

import com.cuzorlabs.ashwanitest.domain.auth.AuthenticateSession
import com.cuzorlabs.ashwanitest.domain.auth.CreateUser
import com.cuzorlabs.ashwanitest.domain.auth.ListAuthenticationAudit
import com.cuzorlabs.ashwanitest.domain.auth.ListUsers
import com.cuzorlabs.ashwanitest.domain.auth.port.SessionStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

class AdminRoutes(private val authenticate: AuthenticateSession, private val sessions: SessionStore, private val createUser: CreateUser, private val listUsers: ListUsers, private val listAudit: ListAuthenticationAudit) {
    fun install(route: Route) = with(route) {
        get("/admin/users") { val (_, user) = call.authenticated(authenticate) ?: return@get; if (!user.isAdministrator) { call.forbidden(); return@get }; call.routeHtml(usersPage(call.request.cookies[CSRF_COOKIE].orEmpty(), listUsers.execute(user))) }
        post("/admin/users") { val (token, user) = call.authenticated(authenticate) ?: return@post; if (!user.isAdministrator) { call.forbidden(); return@post }; if (!call.requireCsrf(token, sessions)) { call.unauthorized(); return@post }; val result = createUser.execute(user, call.receiveParameters()["email"].orEmpty()); val error = result.exceptionOrNull()?.message; call.routeHtml(usersPage(call.request.cookies[CSRF_COOKIE].orEmpty(), listUsers.execute(user), error), if (error == null) HttpStatusCode.Created else HttpStatusCode.BadRequest) }
        get("/admin/audit") { val (_, user) = call.authenticated(authenticate) ?: return@get; if (!user.isAdministrator) { call.forbidden(); return@get }; call.routeHtml(auditPage(listAudit.execute(user, 100, 0))) }
    }
}
