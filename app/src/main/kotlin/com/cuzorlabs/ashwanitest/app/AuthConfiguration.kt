package com.cuzorlabs.ashwanitest.app

import com.cuzorlabs.ashwanitest.domain.auth.model.AuthenticationSettings

data class AuthConfiguration(val jdbcUrl: String, val administratorProvisioningPassword: CharArray, val settings: AuthenticationSettings = AuthenticationSettings()) {
    init { require(jdbcUrl.startsWith("jdbc:sqlite:")) { "Authentication requires a SQLite JDBC URL." }; require(administratorProvisioningPassword.isNotEmpty()) { "Administrator provisioning material is required." } }
}
