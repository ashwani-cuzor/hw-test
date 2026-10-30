package com.cuzorlabs.ashwanitest.app

import com.cuzorlabs.ashwanitest.data.auth.ExposedAuthenticationStore
import com.cuzorlabs.ashwanitest.data.auth.Pbkdf2PasswordHasher
import com.cuzorlabs.ashwanitest.data.database.DatabaseFactory
import com.cuzorlabs.ashwanitest.domain.auth.*
import com.cuzorlabs.ashwanitest.domain.auth.port.SystemClock

class AppComponent(configuration: AuthConfiguration) {
    private val database = DatabaseFactory(configuration.jdbcUrl)
    private val store = ExposedAuthenticationStore(database, configuration.settings)
    private val hasher = Pbkdf2PasswordHasher()
    val authenticationRoutes: com.cuzorlabs.ashwanitest.ui.auth.AuthenticationRoutes
    val adminRoutes: com.cuzorlabs.ashwanitest.ui.auth.AdminRoutes
    init {
        database.migrate()
        EnsurePermanentAdministrator(store, hasher, SystemClock).execute(configuration.administratorProvisioningPassword)
        val authenticate = AuthenticateSession(store, SystemClock)
        val audit = ListAuthenticationAudit(store)
        authenticationRoutes = com.cuzorlabs.ashwanitest.ui.auth.AuthenticationRoutes(SignIn(store, store, store, store, hasher, SystemClock), authenticate, ChangePassword(store, hasher, store, SystemClock), SignOut(store, store, SystemClock), store)
        adminRoutes = com.cuzorlabs.ashwanitest.ui.auth.AdminRoutes(authenticate, store, CreateUser(store, hasher, store, SystemClock), ListUsers(store), audit)
        PurgeAuthenticationAudit(store, configuration.settings, SystemClock).execute()
    }
}
