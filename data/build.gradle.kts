plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":utils"))
    implementation(libs.arrow.core)
    implementation(libs.dagger)
    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxDatetime)
    implementation(libs.kotlinxSerialization)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.migration.core)
    implementation(libs.exposed.migration.jdbc)
    implementation(libs.sqliteDriver)
    implementation(libs.flyway)
    testImplementation(libs.kotlinxCoroutinesTest)
    ksp(libs.dagger.compiler)
}
