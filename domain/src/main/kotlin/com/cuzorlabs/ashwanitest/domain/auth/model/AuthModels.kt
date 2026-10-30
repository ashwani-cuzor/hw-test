package com.cuzorlabs.ashwanitest.domain.auth.model

import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    companion object { fun new(): UserId = UserId(UUID.randomUUID()) }
    override fun toString(): String = value.toString()
}

class EmailAddress private constructor(val value: String) {
    companion object {
        fun parse(raw: String): EmailAddress? {
            val normalized = raw.trim().lowercase(Locale.ROOT)
            return if (normalized.length in 3..254 && EMAIL.matches(normalized)) EmailAddress(normalized) else null
        }
        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
    override fun equals(other: Any?): Boolean = other is EmailAddress && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value
}

data class PasswordVerifier(val algorithm: String, val workFactor: Int, val salt: ByteArray, val digest: ByteArray)

data class User(
    val id: UserId,
    val email: EmailAddress,
    val verifier: PasswordVerifier,
    val isAdministrator: Boolean,
    val mustChangePassword: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class UserSummary(val id: UserId, val email: EmailAddress, val isAdministrator: Boolean, val mustChangePassword: Boolean)
data class AuthenticatedUser(val id: UserId, val email: EmailAddress, val isAdministrator: Boolean, val mustChangePassword: Boolean)

data class SessionToken(val value: String)
data class IssuedSession(val token: SessionToken, val csrfToken: String, val user: AuthenticatedUser)

enum class AuditEventType { SIGN_IN_SUCCEEDED, SIGN_IN_FAILED, SIGN_IN_BLOCK_STARTED, SIGN_IN_BLOCKED, SIGN_OUT, PASSWORD_CHANGED, USER_CREATED }
data class AuthenticationAuditEvent(val type: AuditEventType, val occurredAt: Instant, val actorId: UserId? = null, val subjectId: UserId? = null, val sourceFingerprint: String? = null)

data class AuthenticationSettings(
    val inactivityTimeout: Duration = Duration.ofMinutes(30),
    val failedAttemptLimit: Int = 5,
    val blockDuration: Duration = Duration.ofMinutes(15),
    val auditRetention: Duration = Duration.ofDays(90)
)

const val PERMANENT_ADMINISTRATOR_EMAIL = "ashwani@cuzorlabs.com"
const val INITIAL_PASSWORD = "Password@123"
