package com.cuzorlabs.ashwanitest.domain.auth.port

import com.cuzorlabs.ashwanitest.domain.auth.model.AuthenticationAuditEvent
import com.cuzorlabs.ashwanitest.domain.auth.model.AuthenticatedUser
import com.cuzorlabs.ashwanitest.domain.auth.model.EmailAddress
import com.cuzorlabs.ashwanitest.domain.auth.model.PasswordVerifier
import com.cuzorlabs.ashwanitest.domain.auth.model.SessionToken
import com.cuzorlabs.ashwanitest.domain.auth.model.User
import com.cuzorlabs.ashwanitest.domain.auth.model.UserId
import com.cuzorlabs.ashwanitest.domain.auth.model.UserSummary
import java.time.Instant

interface Clock { fun now(): Instant }
object SystemClock : Clock { override fun now(): Instant = Instant.now() }
interface PasswordHasher { fun hash(password: CharArray): PasswordVerifier; fun matches(password: CharArray, verifier: PasswordVerifier): Boolean }
interface UserStore {
    fun findByEmail(email: EmailAddress): User?
    fun findById(id: UserId): User?
    fun create(user: User): User
    fun updatePassword(id: UserId, verifier: PasswordVerifier, mustChangePassword: Boolean, updatedAt: Instant)
    fun ensurePermanentAdministrator(user: User): User
    fun list(): List<UserSummary>
}
interface SessionStore {
    fun issue(token: SessionToken, csrfToken: String, user: AuthenticatedUser, issuedAt: Instant): Unit
    fun resolve(token: SessionToken, now: Instant): AuthenticatedUser?
    fun csrfMatches(token: SessionToken, csrfToken: String): Boolean
    fun revoke(token: SessionToken, now: Instant)
    fun revokeUser(id: UserId, now: Instant)
}
enum class ThrottleResult { ALLOWED, BLOCKED, BLOCK_STARTED }
interface LoginThrottleStore {
    fun isBlocked(userKey: String?, sourceKey: String, now: Instant): Boolean
    fun failed(userKey: String?, sourceKey: String, now: Instant): ThrottleResult
    fun succeeded(userKey: String?, sourceKey: String)
}
interface AuthenticationAuditStore { fun append(event: AuthenticationAuditEvent); fun list(limit: Int, offset: Int): List<AuthenticationAuditEvent>; fun purgeBefore(instant: Instant) }
interface TransactionRunner { fun <T> inTransaction(action: () -> T): T }
object DirectTransactionRunner : TransactionRunner { override fun <T> inTransaction(action: () -> T): T = action() }
