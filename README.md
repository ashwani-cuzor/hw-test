# ashwanitest

Kotlin web application with server-side authentication for approved members.

## Authentication

The application has no public registration. All routes except `/login` require an authenticated opaque server-side session. New members are created by an administrator in `/admin/users` and must change their initial credential before accessing normal application pages. Administrators can review safe authentication events at `/admin/audit`.

The permanent administrator account is the required protected account. A local development default allows `./gradlew run` to start; production deployment should supply bootstrap material outside source control before first initialization. Run behind HTTPS termination. The application uses secure, HTTP-only, strict same-site session cookies, CSRF validation, no-store responses, and browser security headers.

Run `./gradlew build` to compile and test. The server listens on port 8080 by default when started through Gradle after deployment configuration has been supplied.

## Structure

| Module | Purpose |
|---|---|
| `utils` | Small cross-module helpers. |
| `domain` | Authentication models, policies, use cases, and ports. |
| `data` | SQLite migration, password hashing, and persistence adapters. |
| `ui` | Ktor authentication routes and HTML pages. |
| `app` | Application configuration, composition, and startup. |
