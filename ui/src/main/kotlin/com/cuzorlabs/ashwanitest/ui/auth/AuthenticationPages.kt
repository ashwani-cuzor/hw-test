package com.cuzorlabs.ashwanitest.ui.auth

import com.cuzorlabs.ashwanitest.domain.auth.model.AuthenticationAuditEvent
import com.cuzorlabs.ashwanitest.domain.auth.model.UserSummary

internal fun page(title: String, body: String): String = "<!doctype html><html><head><meta charset=\"utf-8\"><title>$title</title></head><body>$body</body></html>"
internal fun loginPage(error: Boolean = false): String = page("Sign in", "${if (error) "<p>Invalid email or password.</p>" else ""}<h1>Sign in</h1><form method=\"post\" action=\"/login\"><label>Email <input type=\"email\" name=\"email\" required></label><label>Password <input type=\"password\" name=\"password\" required></label><button>Sign in</button></form>")
internal fun passwordPage(csrf: String, errors: List<String> = emptyList()): String = page("Change password", "<h1>Change password</h1>${errors.joinToString("", prefix = "<ul>", postfix = "</ul>") { "<li>${escape(it)}</li>" }}<p>Use 6–128 characters with a letter, digit, and special character.</p><form method=\"post\" action=\"/password/change\"><input type=\"hidden\" name=\"csrf\" value=\"$csrf\"><input type=\"password\" name=\"password\" required><button>Save password</button></form>")
internal fun usersPage(csrf: String, users: List<UserSummary>, error: String? = null): String = page("Users", "<h1>Users</h1>${error?.let { "<p>${escape(it)}</p>" } ?: ""}<ul>${users.joinToString("") { "<li>${escape(it.email.value)}</li>" }}</ul><form method=\"post\" action=\"/admin/users\"><input type=\"hidden\" name=\"csrf\" value=\"$csrf\"><input type=\"email\" name=\"email\" required><button>Create user</button></form>")
internal fun auditPage(events: List<AuthenticationAuditEvent>): String = page("Authentication audit", "<h1>Authentication audit</h1><ul>${events.joinToString("") { "<li>${it.occurredAt} ${it.type} ${it.subjectId ?: ""}</li>" }}</ul>")
private fun escape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
