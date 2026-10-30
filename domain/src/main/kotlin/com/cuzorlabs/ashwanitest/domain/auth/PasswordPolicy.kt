package com.cuzorlabs.ashwanitest.domain.auth

import com.cuzorlabs.ashwanitest.domain.auth.model.INITIAL_PASSWORD

object PasswordPolicy {
    fun violations(password: CharArray): List<String> = buildList {
        if (password.size !in 6..128) add("Password must be between 6 and 128 characters.")
        if (password.none(Char::isLetter)) add("Password must contain a letter.")
        if (password.none(Char::isDigit)) add("Password must contain a digit.")
        if (password.none { !it.isLetterOrDigit() }) add("Password must contain a special character.")
        if (password.concatToString() == INITIAL_PASSWORD) add("Password must not be the initial password.")
    }
}
