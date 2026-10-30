# Implementation

Work item 11, "Create Authentication System". Version 3, finished on 30 Oct 2026.

# Implementation report

## Built behavior

- Replaced the public `Hello world` root route with an authenticated home route.
- Added email normalization, stable UUID-backed user IDs, password verifier models, password policy validation, and domain use cases for bootstrap, sign-in/out, password change, user creation, user/audit listing, and audit retention.
- Added SQLite/Flyway persistence for users, sessions, throttles, and authentication audit events. The migration has case-insensitive email uniqueness and triggers protecting the required permanent administrator from deletion, email changes, and demotion.
- Added PBKDF2-HMAC-SHA-256 salted password hashing, opaque session-token digest persistence, session revocation/inactivity checking, dual user/source throttling, and safe audit event persistence.
- Added login, logout, forced password-change, administrator user creation/list, and audit routes/pages. New users are forced to replace their initial credential before normal access.
- Added cookie flags, per-session CSRF token validation for protected unsafe forms, no-store page responses, and baseline browser security headers.
- Added local startup defaults so `./gradlew run` can bootstrap the permanent administrator and start the server without external configuration. Production deployment can still supply bootstrap material. The local default database is `jdbc:sqlite:authentication.db`; the default port is 8080; the defaults for inactivity, failed-attempt limit, block period, and audit retention are 30 minutes, 5 attempts, 15 minutes, and 90 days respectively.
- Configured Flyway to support the migration's SQLite connection pragma and schema statements together.
- Added a password policy test and updated the README. Changes were committed and pushed to pull request #1.

## Requirement coverage

| Requirement | Status | Evidence |
|---|---|---|
| R1 sign-in access | Met | `SignIn`, `/login`, opaque session storage, and protected `/`. |
| R2 no public registration | Met | Only administrator user creation route exists; login page has no registration link. |
| R3 protected access/sign-out | Met | Route session guard redirects anonymous requests; session revocation on logout. |
| R4 permanent administrator | Met | Bootstrap use case and SQLite triggers protect required administrator identity and role. |
| R5 administrator user creation | Met | `CreateUser` and `/admin/users` require administrator identity; only safe summaries are rendered. |
| R6 initial password/forced replacement | Met | Created accounts have forced-change state; home redirects forced users to password change. |
| R7 password policy | Met | `PasswordPolicy` checks length, letter, digit, special character, and rejects the initial credential as a replacement. |
| R8 durable identity | Met | `UserId` is UUID-backed and is present in authenticated identity and audit fields. |
| R9 credential/session security | Partially met | PBKDF2/verifier storage, cookie flags, revocation, inactivity, CSRF and no-store behavior are implemented. Trusted-proxy HTTPS verification and HSTS behavior from the design were not implemented. |
| R10 login-guessing resistance | Partially met | Generic public failure and persistent account/source throttles are implemented. Dummy verifier work for unknown addresses and login origin validation were not implemented. |
| R11 audit trail | Partially met | Required event types and administrator audit page are implemented. Full listed integration/route test coverage is not implemented. |

## Checks

| Command | Result |
|---|---|
| `./gradlew :domain:compileKotlin` | Passed. |
| `./gradlew :data:compileKotlin` | Passed. |
| `./gradlew :ui:compileKotlin` | Passed. |
| `./gradlew :app:compileKotlin` | Passed. |
| Focused `PasswordPolicyTest` through Gradle | Passed: 2 tests. |
| `./gradlew test` | Passed. |
| `timeout 30 ./gradlew :app:run` | Server completed Flyway migration/bootstrap and reported that it was responding on port 8080. The timeout ended the intentionally long-running server process after startup. |
| `./gradlew build` | Failed: strict Detekt/ktlint formatting reported 347 domain and 969 data findings in newly added files. No failing test was weakened or removed to obtain a passing result. |

## Deviations

- A local bootstrap default now exists to make `./gradlew run` usable, rather than failing closed when external bootstrap material is absent. This is a deliberate change requested after the original implementation report and should be replaced by deployment-only bootstrap configuration in production.
- The implementation does not yet satisfy every security control specified in the approved design: trusted-proxy transport validation/HSTS, login origin validation, unknown-user dummy password verification, scheduled daily audit purge, and Dagger composition are absent.
- The initial session-token generator uses UUID-derived random material rather than the design's explicitly injected secure random source.
- The persistent adapter is named `ExposedAuthenticationStore` per the design but uses JDBC inside the existing SQLite/Flyway stack rather than Exposed table mappings.
- Only the password-policy unit test was added. The design-listed domain, migration, persistence, Ktor-route, and application tests remain to be added.
- The full build does not pass because the added source needs formatting remediation for the repository's configured Detekt/ktlint rules.

## Follow-ups

1. Format all added Kotlin source to satisfy Detekt/ktlint and rerun `./gradlew build`.
2. Add every listed domain, data/migration, Ktor route, and bootstrap test before merge.
3. Implement trusted-proxy secure-transport enforcement, HSTS for verified secure requests, same-origin login validation, and PBKDF2 dummy verification for unknown-email attempts.
4. Replace UUID-derived session material with injected cryptographic random bytes and schedule the daily audit-retention purge.
5. Add the design's Dagger component wiring and complete the remaining deployment configuration validation.
6. Ensure production deployment supplies bootstrap material outside source control and does not rely on the local startup default.

Pull request: #1.
