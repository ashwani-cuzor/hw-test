package com.cuzorlabs.ashwanitest.domain.auth

import com.cuzorlabs.ashwanitest.domain.auth.model.*
import com.cuzorlabs.ashwanitest.domain.auth.port.*
import java.time.Instant
import java.util.Base64
import java.util.UUID

class EnsurePermanentAdministrator(private val users: UserStore, private val hasher: PasswordHasher, private val clock: Clock) {
    fun execute(provisionedPassword: CharArray): User {
        require(PasswordPolicy.violations(provisionedPassword).isEmpty()) { "Administrator provisioning password does not meet policy." }
        val email = requireNotNull(EmailAddress.parse(PERMANENT_ADMINISTRATOR_EMAIL))
        val now = clock.now()
        return users.ensurePermanentAdministrator(User(UserId.new(), email, hasher.hash(provisionedPassword), true, true, now, now))
    }
}
class CreateUser(private val users: UserStore, private val hasher: PasswordHasher, private val audit: AuthenticationAuditStore, private val clock: Clock) {
    fun execute(actor: AuthenticatedUser, rawEmail: String): Result<UserSummary> = runCatching {
        require(actor.isAdministrator) { "Administrator access is required." }
        val email = requireNotNull(EmailAddress.parse(rawEmail)) { "A valid email address is required." }
        require(users.findByEmail(email) == null) { "An account with this email already exists." }
        val now = clock.now()
        val user = users.create(User(UserId.new(), email, hasher.hash(INITIAL_PASSWORD.toCharArray()), false, true, now, now))
        audit.append(AuthenticationAuditEvent(AuditEventType.USER_CREATED, now, actor.id, user.id))
        UserSummary(user.id, user.email, user.isAdministrator, user.mustChangePassword)
    }
}
class ChangePassword(private val users: UserStore, private val hasher: PasswordHasher, private val audit: AuthenticationAuditStore, private val clock: Clock) {
    fun execute(actor: AuthenticatedUser, password: CharArray): List<String> {
        val violations = PasswordPolicy.violations(password)
        if (violations.isNotEmpty()) return violations
        val now = clock.now()
        users.updatePassword(actor.id, hasher.hash(password), false, now)
        audit.append(AuthenticationAuditEvent(AuditEventType.PASSWORD_CHANGED, now, actor.id, actor.id))
        return emptyList()
    }
}
sealed interface SignInResult { data class Success(val session: IssuedSession): SignInResult; data object Failure: SignInResult }
class SignIn(private val users: UserStore, private val sessions: SessionStore, private val throttles: LoginThrottleStore, private val audit: AuthenticationAuditStore, private val hasher: PasswordHasher, private val clock: Clock) {
    fun execute(rawEmail: String, password: CharArray, sourceFingerprint: String): SignInResult {
        val now = clock.now(); val email = EmailAddress.parse(rawEmail); val user = email?.let(users::findByEmail); val key = email?.value
        if (throttles.isBlocked(key, sourceFingerprint, now)) { audit.append(AuthenticationAuditEvent(AuditEventType.SIGN_IN_BLOCKED, now, user?.id, user?.id, sourceFingerprint)); return SignInResult.Failure }
        val valid = user != null && hasher.matches(password, user.verifier)
        if (!valid) {
            val outcome = throttles.failed(key, sourceFingerprint, now)
            audit.append(AuthenticationAuditEvent(AuditEventType.SIGN_IN_FAILED, now, user?.id, user?.id, sourceFingerprint))
            if (outcome == ThrottleResult.BLOCK_STARTED) audit.append(AuthenticationAuditEvent(AuditEventType.SIGN_IN_BLOCK_STARTED, now, user?.id, user?.id, sourceFingerprint))
            return SignInResult.Failure
        }
        throttles.succeeded(key, sourceFingerprint)
        val identity = AuthenticatedUser(user.id, user.email, user.isAdministrator, user.mustChangePassword)
        val token = SessionToken(randomToken()); val csrf = randomToken(); sessions.issue(token, csrf, identity, now)
        audit.append(AuthenticationAuditEvent(AuditEventType.SIGN_IN_SUCCEEDED, now, user.id, user.id, sourceFingerprint))
        return SignInResult.Success(IssuedSession(token, csrf, identity))
    }
    private fun randomToken(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().toByteArray()) + UUID.randomUUID()
}
class AuthenticateSession(private val sessions: SessionStore, private val clock: Clock) { fun execute(token: String?): AuthenticatedUser? = token?.let { sessions.resolve(SessionToken(it), clock.now()) } }
class SignOut(private val sessions: SessionStore, private val audit: AuthenticationAuditStore, private val clock: Clock) { fun execute(token: String, actor: AuthenticatedUser) { val now = clock.now(); sessions.revoke(SessionToken(token), now); audit.append(AuthenticationAuditEvent(AuditEventType.SIGN_OUT, now, actor.id, actor.id)) } }
class ListUsers(private val users: UserStore) { fun execute(actor: AuthenticatedUser): List<UserSummary> { require(actor.isAdministrator) { "Administrator access is required." }; return users.list() } }
class ListAuthenticationAudit(private val audit: AuthenticationAuditStore) { fun execute(actor: AuthenticatedUser, limit: Int, offset: Int): List<AuthenticationAuditEvent> { require(actor.isAdministrator) { "Administrator access is required." }; return audit.list(limit, offset) } }
class PurgeAuthenticationAudit(private val audit: AuthenticationAuditStore, private val settings: AuthenticationSettings, private val clock: Clock) { fun execute() = audit.purgeBefore(clock.now().minus(settings.auditRetention)) }
