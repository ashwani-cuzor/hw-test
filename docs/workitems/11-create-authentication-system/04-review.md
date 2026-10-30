# Review

Work item 11, "Create Authentication System". Version 3, finished on 30 Oct 2026.

# Review — Authentication System

## Verdict

**Does not meet the approved requirements and design.** The new local-startup change introduces a critical credential-provisioning regression: any deployment started without a system property creates the permanent administrator with the known initial credential embedded in source. The previous functional/security gaps, missing test coverage, and failing quality gate also remain.

## Findings

1. **Open — Production can bootstrap the permanent administrator with the known initial credential from source (R4, R9/design).**  
   **Files:** `app/src/main/kotlin/com/cuzorlabs/ashwanitest/app/App.kt`; `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/AuthenticationUseCases.kt`  
   **Reason:** `main` falls back to `INITIAL_PASSWORD` when `auth.bootstrap.password` is absent. There is no development-profile or deployment-mode guard, so this fallback applies equally to an internet-facing production process. `EnsurePermanentAdministrator` was changed to explicitly accept that otherwise-disallowed initial password for the administrator. This contradicts the approved deployment-only, fail-closed bootstrap rule and allows an attacker who knows the specified initial credential to race the legitimate administrator on a newly initialized public deployment. Local convenience must not create an insecure production default.

2. **Open — Forced password change is bypassable for an administrator (R6).**  
   **Files:** `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AdminRoutes.kt`; `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AuthenticationRoutes.kt`  
   **Reason:** The home route redirects a `mustChangePassword` user, but `/admin/users` and `/admin/audit` check only `isAdministrator`. The newly seeded permanent administrator has `mustChangePassword=true`, yet can directly access the dashboard and create users before changing the initial password. R6 permits only password change and sign-out until replacement succeeds.

3. **Open — Secure transport enforcement and required browser headers are missing (R9/design).**  
   **Files:** `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/SecurityHeaders.kt`; `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AuthenticationRoutes.kt`; `app/src/main/kotlin/com/cuzorlabs/ashwanitest/app/App.kt`  
   **Reason:** `installSecurityHeaders()` is a no-op. `routeHtml` sets some headers, but not the required restrictive referrer policy or HSTS. The application has no trusted-proxy configuration, forwarded-scheme validation, or rejection of an insecure public request. `Secure` cookie attributes alone do not enforce the approved HTTPS deployment/verification design.

4. **Open — Login has neither same-origin validation nor equivalent unknown-user verifier work (R9, R10/design).**  
   **Files:** `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AuthenticationRoutes.kt`; `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/AuthenticationUseCases.kt`  
   **Reason:** `POST /login` accepts form submissions without an Origin or CSRF check. For an unknown email, `SignIn` skips PBKDF2; for a known email with a wrong password, it runs PBKDF2. This creates the account-existence timing difference that the approved dummy-verifier behavior is intended to avoid.

5. **Open — Login blocking, session creation, and required audit events are not atomic (R10, R11/design).**  
   **Files:** `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/AuthenticationUseCases.kt`; `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth/port/AuthenticationPorts.kt`; `data/src/main/kotlin/com/cuzorlabs/ashwanitest/data/auth/ExposedAuthenticationStore.kt`  
   **Reason:** `SignIn` separately checks/updates throttles, issues sessions, and appends audit events. The defined `TransactionRunner` is never used. A concurrent failed request can begin a block after a valid request passed its initial check but before it creates its session; that valid request can then clear the throttle and succeed. Also, a user/session mutation can commit if a following audit append fails. This does not implement the approved immediate transaction and conditional-update behavior.

6. **Open — Audit source handling, retention scheduling, and dashboard paging do not meet the design.**  
   **Files:** `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AuthenticationRoutes.kt`; `data/src/main/kotlin/com/cuzorlabs/ashwanitest/data/auth/ExposedAuthenticationStore.kt`; `app/src/main/kotlin/com/cuzorlabs/ashwanitest/app/AppComponent.kt`; `ui/src/main/kotlin/com/cuzorlabs/ashwanitest/ui/auth/AdminRoutes.kt`  
   **Reason:** A raw `remoteHost` is persisted as a source fingerprint rather than a non-reversible fingerprint. Audit retention runs only during component construction, not daily. `/admin/audit` always reads the first 100 rows and provides no page/offset mechanism, preventing review of older events through the required paged dashboard.

7. **Open — Full build quality gate fails.**  
   **Files:** Added Kotlin files under `domain/src/main/kotlin/com/cuzorlabs/ashwanitest/domain/auth` and `data/src/main/kotlin/com/cuzorlabs/ashwanitest/data/auth`  
   **Reason:** `./gradlew build` fails at `:domain:detektMain` with 349 issues and at `:data:detektMain` with 979 issues. The approved implementation plan requires the complete build, including static quality checks, to pass.

8. **Open — Required automated coverage is absent.**  
   **Files:** `domain/src/test/kotlin/com/cuzorlabs/ashwanitest/domain/auth/PasswordPolicyTest.kt`; missing planned tests in `data`, `ui`, and `app`  
   **Reason:** The repository contains only two password-policy tests. The approved test plan calls for domain sign-in/create/change tests; migration, session, throttle, and audit integration tests; Ktor access/CSRF/transport route tests; and bootstrap tests. The new bootstrap behavior, forced-change gate, administrator authorization, sign-out reuse prevention, audit retention, concurrent throttling, and transport behavior are not independently verified.

No findings were verified as **Fixed** or designated **Won't fix** in this review.

## Checks

| Check | Result |
|---|---|
| `git status` and commit review | No uncommitted changes. Reviewed `53a2a19` and its predecessor changes against `main`. |
| `git diff main` | Reviewed authentication, persistence, routes, migration, configuration, README, and test changes. Confirmed the new source-level bootstrap fallback and the unchanged gaps listed above. |
| `./gradlew test` | **Passed: 2 tests.** Test-file inventory contains only `PasswordPolicyTest.kt`. |
| `./gradlew build` | **Failed.** `:domain:detektMain` reported 349 issues and `:data:detektMain` reported 979 issues. |
| Static review of route guards, startup composition, session/throttle persistence, and audit flow | Found the open issues above. |

I could not verify deployment-edge HTTPS termination/trusted-proxy behavior, browser behavior through a correctly configured HTTPS endpoint, secure operational provisioning, or deployed concurrent SQLite behavior. The absent integration, route, and application tests also prevent independent execution of most approved acceptance scenarios.
