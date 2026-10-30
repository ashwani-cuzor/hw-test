package com.cuzorlabs.ashwanitest.data.auth

import com.cuzorlabs.ashwanitest.domain.auth.model.PasswordVerifier
import com.cuzorlabs.ashwanitest.domain.auth.port.PasswordHasher
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Pbkdf2PasswordHasher(private val iterations: Int = 210_000, private val random: SecureRandom = SecureRandom()) : PasswordHasher {
    override fun hash(password: CharArray): PasswordVerifier {
        val salt = ByteArray(16).also(random::nextBytes)
        return PasswordVerifier("PBKDF2WithHmacSHA256", iterations, salt, derive(password, salt, iterations))
    }
    override fun matches(password: CharArray, verifier: PasswordVerifier): Boolean = MessageDigest.isEqual(derive(password, verifier.salt, verifier.workFactor), verifier.digest)
    private fun derive(password: CharArray, salt: ByteArray, workFactor: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, workFactor, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
