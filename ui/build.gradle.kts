plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinPluginSerialization)
    `java-test-fixtures`
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":utils"))
    implementation(libs.arrow.core)
    implementation(libs.dagger)
    implementation(libs.kotlinxDatetime)
    // `ktor-server-html-builder` brings in kotlinx.html.
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.html.builder)
    ksp(libs.dagger.compiler)
    testImplementation(libs.ktor.server.test.host)
    testFixturesImplementation(project(":domain"))
    testFixturesImplementation(libs.arrow.core)
    testFixturesImplementation(libs.ktor.server.core)
    testFixturesImplementation(libs.ktor.server.resources)
}
