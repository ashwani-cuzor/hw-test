# Design

Work item 11, "Create Authentication System". Version 1, approved by Ashwani on 30 Oct 2026.

# Design — Authentication System

## Summary

**Recommended design: server-side opaque-session authentication.** The application will store approved users, password verifiers, session records, login-throttle state, and an authentication audit trail in SQLite. Ktor routes issue only a high-entropy opaque session cookie; the database remains the authority for identity, sign-out, inactivity expiry, forced password change, and administrator status.

A signed-in request resolves to an `AuthenticatedUser` containing an immutable `UserId`. Future domain use cases receive that ID explicitly when they create or update records; this change deliberately adds no audit columns to business modules that do not yet exist.

No public registration route will exist. The permanent account for `ashwani@cuzorlabs.com` is seeded and protected both in domain rules and database triggers. A new member receives the specified initial credential only as a stored verifier, is marked `mustChangePassword`, and can use only the change-password and sign-out routes until completing a compliant replacement. Creation responses and audit events never include credential material.

The mandated shared initial credential is inherently weaker than a per-user random activation credential: a party who knows it can race a newly created member to sign in. The baseline implements it exactly because it is an approved rule, mitigated by forced replacement, short lockouts, audit, and no credential display. A future change to unique, expiring activation credentials is the recommended security improvement.

A1 is applied: deployment configuration supplies the one-time bootstrap material for the permanent administrator and the operational settings. Defaults are 30-minute inactivity, five failures, 15-minute block, and 90-day audit retention. The application fails closed if it must create the administrator but provisioning material is unavailable. HTTPS is terminated and enforced by deployment; the application accepts forwarded scheme/client information only from configured trusted infrastructure and rejects public insecure requests.

No new library is needed. The existing Kotlin, Ktor, Exposed, SQLite, Flyway, Dagger, `kotlinx.datetime`, and JDK 25 stack is sufficient. Password verification uses JDK PBKDF2-HMAC-SHA-256 with a per-user cryptographic salt, configured work factor, and constant-time comparison.

## Contracts

### Domain models and policies (`domain`)

* `UserId` is a UUID-backed value type. It is generated once, never derived from email, and is the value future records use for `createdBy` and `modifiedBy`.
* `EmailAddress` validates a syntactically usable address and normalizes it with locale-independent lower casing. Equality and persistence use the normalized value, making email uniqueness case-insensitive.
* `User` exposes `id`, normalized email, `isAdministrator`, `mustChangePassword`, and timestamps. It never exposes a verifier to UI code.
* `PasswordVerifier` is an opaque domain value containing algorithm identifier, work factor, salt, and derived verifier bytes. There is no reversible credential field.
* `AuthenticatedUser` is the request identity: `UserId`, email, administrator flag, and `mustChangePassword`. It is placed in a Ktor call attribute only after session validation.
* `PasswordPolicy` accepts 6–128 characters with at least one letter, digit, and special character; it returns individual missing-rule messages. It also refuses the defined initial credential as a replacement. It never returns entered text.
* `PasswordHasher` hashes/verifies character input and has a PBKDF2 implementation in `data`. The app uses a dummy verifier for unknown-email attempts so lookup success does not substantially change verification work.
* `AuthenticationSettings` contains duration/limit values, configured public origin, trusted proxy setting, and retention duration. It is supplied at composition time rather than hard-coded.
* `Clock` is injected so expiry, throttling, and audit behavior are deterministic in tests.

### Use cases and ports (`domain`)

Ports remain framework-free:

* `UserStore`: find by normalized email or ID, create a member, ensure the permanent administrator, and list safe user summaries.
* `SessionStore`: issue, resolve/touch, revoke one session, revoke a user’s sessions, and expire stale sessions. It stores only a digest of an opaque token.
* `LoginThrottleStore`: atomically inspect and update per-user and per-source failure windows and block periods.
* `AuthenticationAuditStore`: append safe events, page events for administrators, and purge records older than configured retention.
* `TransactionRunner`: runs the multi-store portions of sign-in, creation, password change, and sign-out atomically.

Use cases are:

* `EnsurePermanentAdministrator` runs at startup after migration. It creates the required administrator only through the one-time deployment provisioning path and always enforces administrator status and forced initial replacement for that account.
* `SignIn` normalizes input, validates both throttle keys, performs equivalent verifier work for an unknown email, and emits either an opaque session or one generic failure result. A failed lookup/password and an active temporary block have the same user-facing outcome. Failures increment both known-account and source keys when applicable; crossing a threshold records the block event.
* `AuthenticateSession` validates the cookie-token digest, revocation state, and inactivity time; it touches valid sessions and returns `AuthenticatedUser`.
* `ChangePassword` is available to a forced-change user and updates the verifier and flag atomically. It records `PASSWORD_CHANGED`; optional later voluntary changes can use the same operation. It does not accept or log a prior credential.
* `SignOut` revokes the current session and records `SIGN_OUT`.
* `CreateUser` is administrator-only. It validates normalized uniqueness, creates a stable ID with the required initial verifier and `mustChangePassword=true`, and records `USER_CREATED` with actor and subject IDs. Its result has only safe user summary fields.
* `ListUsers` and `ListAuthenticationAudit` are administrator-only read use cases. The latter is paged and exposes event type, time, actor/subject IDs where known, and a non-reversible source fingerprint.
* `PurgeAuthenticationAudit` executes at startup and then daily, removing entries older than the retention setting.

Audit event types are `SIGN_IN_SUCCEEDED`, `SIGN_IN_FAILED`, `SIGN_IN_BLOCK_STARTED`, `SIGN_IN_BLOCKED`, `SIGN_OUT`, `PASSWORD_CHANGED`, and `USER_CREATED`. They contain event time, known actor/subject IDs, and an optional one-way source fingerprint—not credential text, verifier data, request bodies, or raw input values.

### Persistent schema and migration (`data`)

Flyway migration `V1__create_authentication_tables.sql` creates:

| Table | Essential columns and constraints |
|---|---|
| `users` | UUID `id` primary key; lowercase unique `email`; verifier algorithm/work-factor/salt/digest; `is_administrator`; `must_change_password`; created/updated timestamps. |
| `auth_sessions` | Digest of the random session token as primary key; user ID foreign key; issued, last-seen, expiry/revocation timestamps; and a digest of the CSRF token. |
| `login_throttles` | Scope (`USER` or `SOURCE`), subject fingerprint, window start, failure count, and block-until, unique by scope and subject. |
| `auth_audit_events` | Event ID, event type, occurred time, nullable actor/subject IDs, and nullable source fingerprint, with indexes for time and subject queries. |

The migration enables foreign keys and has SQLite triggers that reject deletion of the permanent administrator, changing its email, or clearing its administrator flag. The use case repeats those checks so a friendly error is produced before the database guard. There is deliberately no user-delete, user-demotion, reset, or public-registration operation in this work item.

`DatabaseFactory` opens the configured SQLite database, applies Flyway before repositories are used, and enables the SQLite settings required for transactional integrity. `ExposedAuthenticationStore` implements the ports. Its sign-in update uses an immediate transaction and conditional updates so concurrent attempts cannot bypass the account/source limit or create a session after a newly observed block.

### HTTP routes and request rules (`ui`)

All content is server-rendered with existing Ktor HTML support.

| Route | Access and behavior |
|---|---|
| `GET /login` | Anonymous sign-in form. There is no registration link. |
| `POST /login` | Anonymous, origin/CSRF protected. On success sets a fresh opaque cookie and redirects to `/password/change` when required, otherwise `/`. On every failed or blocked attempt renders the same generic failure response. |
| `POST /logout` | Authenticated user; validates CSRF, revokes session, expires cookie, redirects to `/login`. |
| `GET /` | Authenticated protected home (replacing the current public hello response). |
| `GET /password/change`, `POST /password/change` | Authenticated user; available during forced replacement. The POST validates policy and CSRF; successful completion unlocks normal routes. |
| `GET /admin/users`, `POST /admin/users` | Administrator-only list and create form. The form accepts an email only, and neither page nor response displays initial credential material. |
| `GET /admin/audit` | Administrator-only paged audit-trail view. |

`AuthenticationRouteGuard` resolves a valid session before protected handlers. Unauthenticated GETs redirect to the sign-in route without reflecting an arbitrary return URL; protected unsafe requests receive an unauthorized response. `PasswordChangeGuard` redirects a forced-change identity away from every protected route except change-password and sign-out. `AdministratorGuard` returns forbidden for a non-administrator.

`SecurityHeaders` applies `Cache-Control: no-store` to sign-in and protected pages and adds a restrictive content-security policy, frame denial, `nosniff`, restrictive referrer policy, and HSTS only for verified secure requests. Session cookies are `HttpOnly`, `Secure`, `SameSite=Strict`, path-scoped, and contain only a random token. A per-session CSRF token is rendered in protected forms and compared by digest; login also validates the configured same-origin request origin. Unsafe methods require the CSRF/origin checks. Secure public transport is enforced at the deployment edge and verified by the app’s trusted-proxy policy.

Dagger’s application component composes configuration, clock, secure random source, database/migration factory, stores, use cases, and routes. Tests compose the same contracts with temporary SQLite databases, a fixed clock, and deterministic non-production random sources.

## Files

| File | Change | Requirement served |
|---|---|---|
| `gradle/libs.versions.toml` | No change; existing stack is sufficient. | Avoids an unnecessary crypto/session dependency. |
| `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/model/AuthModels.kt` | Add value types, safe user summaries, authenticated identity, verifier, audit event, and settings models. | R4, R8, R9, R11. |
| `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/port/AuthenticationPorts.kt` | Add stores, transaction runner, clock, and password-hasher contracts. | R8–R11. |
| `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/PasswordPolicy.kt` | Add replacement-policy validation and safe violation results. | R6–R7. |
| `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/AuthenticationUseCases.kt` | Add bootstrap, sign-in/out, session authentication, password change, user create/list, audit list/purge use cases. | R1, R3–R11. |
| `data/src/main/resources/db/migration/V1__create_authentication_tables.sql` | Add auth schema, indexes, permanent-admin triggers, and foreign-key setup. | R4, R5, R8–R11. |
| `data/src/main/kotlin/com/cuzorlabs/ashwanitest/data/database/DatabaseFactory.kt` | Configure SQLite, Flyway migration, and Exposed transaction setup. | Durable accounts and audit state. |
| `data/src/main/kotlin/com/cuzorlabs/ashwanitest/data/auth/ExposedAuthenticationStore.kt` | Implement all authentication stores, atomic throttle/session operations, and audit retention query. | R1, R4–R6, R8–R11. |
| `data/src/main/kotlin/com/cuzorlabs/ashwanitest/data/auth/Pbkdf2PasswordHasher.kt` | Add salted JDK verifier implementation, dummy verification, and constant-time compare. | R7, R9–R10. |
| `app/src/main/kotlin/com/cuzorlabs/ashwanitest/app/AuthConfiguration.kt` | Define validated deployment configuration and fail-closed bootstrap validation without embedding provisioning material. | R4, R9–R10 and A1. |
| `app/src/main/kotlin/com/cuzorlabs/ashwanitest/app/AppComponent.kt` | Add Dagger composition for stores, use cases, Ktor routes, and lifecycle jobs. | All runtime wiring. |
| `app/src/main/kotlin/com/cuzorlabs/ashwanitest/app/App.kt` | Replace public-only routing with migration/bootstrap, security middleware, auth routes, and retention-job lifecycle. | R1–R11. |
| `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AuthenticationRouteGuard.kt` | Add session, forced-change, administrator, CSRF, origin, and transport guards. | R3, R4–R6, R9–R10. |
| `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AuthenticationRoutes.kt` | Add login, logout, protected home, and change-password handlers. | R1–R3, R6–R10. |
| `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AdminRoutes.kt` | Add administrator user and audit handlers. | R4, R5, R11. |
| `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AuthenticationPages.kt` | Add safe HTML forms/pages with CSRF fields, policy help, and no registration/credential echo. | R2, R5–R7, R9. |
| `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/SecurityHeaders.kt` | Add cache and browser security headers. | R3, R9. |
| `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/HelloRoutes.kt` | Remove; `/` becomes protected home in authentication routes. | R3. |
| `README.md` | Replace the public hello-world description with safe operational prerequisites and route behavior; do not include provisioning material. | R9 and A1. |

Test files follow the same packages: `domain/.../auth`, `data/.../auth`, `ui/.../auth`, and `app/.../app`. The current `HelloRoutesTest.kt` is removed because its public root-route behavior is intentionally invalid after this change.

## Tests

### Domain unit tests

| Test name | Coverage |
|---|---|
| `EmailAddressTest.normalizes_case_and_rejects_invalid_addresses` | R5 and business rule 1. |
| `PasswordPolicyTest.reports_each_missing_requirement_without_echoing_input` | R7 rejection behavior. |
| `PasswordPolicyTest.accepts_a_compliant_replacement` | R7 acceptance behavior. |
| `CreateUserUseCaseTest.creates_a_unique_member_requiring_password_change` | R5, R6, R8. |
| `CreateUserUseCaseTest.rejects_a_non_administrator` | R5. |
| `ChangePasswordUseCaseTest.clears_the_forced_change_flag_only_after_policy_passes` | R6–R7. |
| `SignInUseCaseTest.returns_the_same_public_failure_for_unknown_and_wrong_credentials` | R1 and R10. |
| `SignInUseCaseTest.blocks_after_the_configured_account_or_source_limit` | R10 and A1. |
| `SignInUseCaseTest.returns_a_stable_user_id_in_authenticated_identity` | R8. |

### Data and migration integration tests

| Test name | Coverage |
|---|---|
| `AuthenticationMigrationTest.creates_all_authentication_tables_and_indexes` | R1, R8–R11 persistence. |
| `AuthenticationMigrationTest.enforces_case_insensitive_email_uniqueness` | Business rule 1. |
| `PermanentAdministratorStoreTest.refuses_delete_demotion_and_email_change_for_the_permanent_administrator` | R4 and business rule 2. |
| `PermanentAdministratorStoreTest.bootstraps_the_required_administrator_as_forced_change` | R4, R6, A1. |
| `Pbkdf2PasswordHasherTest.uses_unique_salts_and_verifies_only_the_matching_credential` | R9. |
| `SessionStoreTest.revoked_or_idle_sessions_do_not_authenticate` | R3 and R9. |
| `LoginThrottleStoreTest.concurrent_failures_cannot_exceed_the_configured_limit` | R10. |
| `AuthenticationAuditStoreTest.records_required_events_without_credential_columns_or_values` | R11 and R9. |
| `AuthenticationAuditStoreTest.purges_events_past_the_retention_period` | A1 audit retention. |

### Ktor route and application tests

| Test name | Coverage |
|---|---|
| `AuthenticationRoutesTest.redirects_anonymous_direct_access_to_login_without_protected_content` | R3. |
| `AuthenticationRoutesTest.has_no_public_registration_route_or_link` | R2. |
| `AuthenticationRoutesTest.accepts_valid_sign_in_and_sets_an_opaque_secure_cookie` | R1 and R9. |
| `AuthenticationRoutesTest.renders_identical_failure_for_unknown_wrong_and_blocked_sign_in` | R10. |
| `AuthenticationRoutesTest.sign_out_revokes_the_session_and_prevents_reuse` | R3 and R9. |
| `ForcedPasswordChangeRoutesTest.redirects_a_new_member_to_change_password_and_blocks_home_and_admin` | R6. |
| `ForcedPasswordChangeRoutesTest.accepts_a_compliant_replacement_then_unlocks_protected_routes` | R6–R7. |
| `AdminUserRoutesTest.allows_only_an_administrator_to_list_or_create_users` | R5. |
| `AdminUserRoutesTest.creation_response_never_contains_credential_material` | R5 and R9. |
| `AdminAuditRoutesTest.allows_only_administrators_to_view_timestamped_user_and_sign_in_events` | R11 and A1. |
| `RequestSecurityTest.rejects_missing_or_invalid_csrf_on_unsafe_routes` | R9. |
| `RequestSecurityTest.applies_no_store_cookie_flags_and_browser_security_headers` | R9. |
| `RequestSecurityTest.rejects_untrusted_insecure_public_requests` | R9 and A1. |
| `ApplicationBootstrapTest.fails_closed_when_required_administrator_provisioning_cannot_be_completed` | R4 and A1. |

## Slices

1. **Domain security contracts and policy**
   * Add models, ports, clock abstraction, password policy, and pure use cases with in-memory fakes in tests.
   * Run `:domain:test` for `EmailAddressTest`, `PasswordPolicyTest`, `CreateUserUseCaseTest`, `ChangePasswordUseCaseTest`, and `SignInUseCaseTest`.
   * Check: invalid replacement input reports only missing rules; all identity-bearing results contain a stable ID and no verifier is exposed.

2. **Durable account, verifier, and permanent-administrator foundation**
   * Add Flyway migration, database factory, PBKDF2 hasher, Exposed stores, bootstrap behavior, and permanent-admin triggers.
   * Run `:data:test` for migration, permanent-admin, and hasher tests.
   * Check: a temporary SQLite database has all schema constraints; case variants cannot coexist; direct demotion/delete SQL for the permanent account fails; stored values are verifiers rather than recoverable credentials.

3. **Sessions, throttling, and audit transaction**
   * Implement opaque token digest storage, inactivity/revocation, atomic dual-key throttle updates, sign-in/out use cases, audit append/query/retention, and scheduled purge lifecycle contract.
   * Run `:domain:test :data:test` for session, throttle, audit, and retention tests.
   * Check: valid sessions authenticate, signed-out/idle sessions do not; failure behavior is generic; concurrent failures result in a temporary block and audit records are safe.

4. **Protected Ktor authentication experience**
   * Add composition, authentication guards, sign-in/sign-out/home/change-password routes and pages; remove `HelloRoutes`; apply cookie, CSRF, origin, cache, header, and transport checks.
   * Run `:ui:test :app:test` for authentication, forced-change, and request-security tests.
   * Check with Ktor test host: anonymous protected access exposes no protected content; a newly created member cannot bypass replacement; sign-out prevents cookie reuse.

5. **Administrator dashboard and operational hardening**
   * Add administrator list/create/audit pages and routes, validated deployment settings, startup bootstrap, audit cleanup job, and README operational documentation.
   * Run all listed route/application tests, then `./gradlew build`.
   * Check: only administrator sessions can create/list users or inspect audit; creation does not reveal credential material; absent required bootstrap provisioning fails closed; the full build, test, and detekt suite pass.

## Simpler alternatives

| Alternative | What it saves | What it gives up | Recommendation |
|---|---|---|---|
| **Recommended: opaque server-side cookie session with SQLite stores** | Uses existing dependencies and one database; no identity provider, mail service, or new crypto package. | Nothing required by the approved scope. | Adopt. It is the simplest design that still provides revocation, inactivity expiry, forced-change gating, dual-key throttling, and an auditable trail. |
| Signed self-contained cookie session with no session table | Removes the session table and lookup per request. | Cannot reliably revoke a stolen cookie at sign-out, immediately enforce all server-side expiry changes, or retain session lifecycle state; weakens R3/R9. | Do not use. |
| Store only users and enforce sign-in throttling in process memory | Removes throttle and audit tables. | Resets limits on restart, does not work across processes, loses required audit events and retention, and is easier to bypass; fails R10/R11. | Do not use. |
| Administrator creation only, with audit review performed directly in the database | Removes `/admin/audit` page and route. | Requires direct database access for an authorized reviewer and gives no application authorization boundary for the trail. | Do not use under A1; it is viable only if a person changes the audit-review requirement. |

A security improvement—not a simpler alternative—is to replace the mandated shared first credential with a unique, cryptographically random, expiring activation credential delivered through an approved separate channel. That removes the known-first-credential race while keeping the rest of this design unchanged; it should be prioritized if the fixed-value rule can be revised.
