package com.cuzorlabs.ashwanitest.domain.auth

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordPolicyTest {
    @Test
    fun `reports each missing requirement without echoing input`() {
        val violations = PasswordPolicy.violations("abcdef".toCharArray())
        assertContains(violations.joinToString(), "digit")
        assertContains(violations.joinToString(), "special")
        assertFalse(violations.joinToString().contains("abcdef"))
    }
    @Test fun `accepts a compliant replacement`() = assertTrue(PasswordPolicy.violations("Abcde1!".toCharArray()).isEmpty())
}
