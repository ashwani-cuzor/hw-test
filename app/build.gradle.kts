plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply the Application plugin to add support for building an executable JVM application.
    application
    alias(libs.plugins.ksp)
    `java-test-fixtures`
}

dependencies {
    // Project "app" depends on every other project. (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":utils"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":ui"))
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.conditional.headers)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.logback.classic)
    // Architecture tests; the shared scopes in test fixtures expose Konsist types.
    testFixturesApi(libs.konsist)
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.cuzorlabs.ashwanitest.app.AppKt`.)
    mainClass = "com.cuzorlabs.ashwanitest.app.AppKt"
}
