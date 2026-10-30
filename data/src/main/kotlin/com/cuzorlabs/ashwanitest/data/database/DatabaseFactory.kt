package com.cuzorlabs.ashwanitest.data.database

import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.DriverManager

class DatabaseFactory(private val jdbcUrl: String) {
    fun migrate() {
        Flyway.configure().dataSource(jdbcUrl, null, null).locations("classpath:db/migration").mixed(true).load().migrate()
    }
    fun connection(): Connection = DriverManager.getConnection(jdbcUrl).also { connection -> connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") } }
}
