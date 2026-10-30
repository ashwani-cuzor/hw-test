// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
    kotlin("jvm")
    // Static analysis and formatting rules; configured in `config/detekt/detekt.yml`.
    id("dev.detekt")
}

group = "com.cuzorlabs.ashwanitest"

kotlin {
    // Use a specific Java version to make it easier to work in different environments.
    jvmToolchain(25)
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    // kotlin-test on JUnit 5 for every module's unit tests.
    testImplementation(kotlin("test"))
    // ktlint formatting rules run as part of detekt.
    detektPlugins(libs.findLibrary("detektKtlintWrapper").get())
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootDir.resolve("config/detekt/detekt.yml"))
    // Any finding fails the build, whatever its severity.
    failOnSeverity = FailOnSeverity.Info
}

// `check` already runs the plain `detekt` task. Also run the per-compilation tasks (`detektMain`, `detektTest`,
// `detektTestFixtures`), because only they have type resolution, which rules such as UnsafeCallOnNullableType (`!!`)
// and ForbiddenMethodCall (`getOrThrow`) need.
val typeResolvedDetektTasks = tasks.withType<Detekt>().matching { task ->
    task.name != "detekt" && !task.name.endsWith("SourceSet")
}
tasks.named("check") {
    dependsOn(typeResolvedDetektTasks)
}

tasks.withType<Test>().configureEach {
    // Configure all test Gradle tasks to use JUnitPlatform.
    useJUnitPlatform()

    // Log information about all test results, not only the failed ones.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}
