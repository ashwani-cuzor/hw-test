# ashwanitest

Kotlin (`com.cuzorlabs.ashwanitest`) web application built with Gradle.

* Run `./gradlew run` to build and run the application. It listens on port 8080 (or the `PORT` variable) and shows
  "Hello world" at `/`.
* Run `./gradlew build` to only build the application.
* Run `./gradlew check` to run all checks, including tests and detekt.
* Run `./gradlew clean` to clean all build outputs.

## Structure

The build is a multi-module Gradle project. Shared build logic lives in a convention plugin in `buildSrc`, and all
dependency versions are declared in the version catalog `gradle/libs.versions.toml`.

| Module   | May depend on             | Purpose                                                         |
|----------|---------------------------|-----------------------------------------------------------------|
| `utils`  | nothing                   | Small cross-module helpers.                                     |
| `domain` | `utils`                   | Business models, use cases and ports.                           |
| `data`   | `domain`, `utils`         | Persistence (Exposed, SQLite, Flyway) and port implementations. |
| `ui`     | `domain`, `utils`         | Ktor routes and `kotlinx.html` pages.                           |
| `app`    | every module              | Configuration, Dagger component and startup.                    |

## Stack

Kotlin, Ktor (server and client), `kotlinx.html`, Exposed with SQLite and Flyway, Arrow, Dagger (KSP), kotlinx
(coroutines, datetime, serialization), detekt and Konsist.
