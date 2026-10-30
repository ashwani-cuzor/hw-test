# Requirements

Work item 11, "Create Authentication System". Version 1, approved by Ashwani on 30 Oct 2026.

# Requirements — Authentication System

## Purpose and goals

The internet-exposed application needs to admit only approved members and prevent unauthenticated visitors from accessing protected information or functions. Success means an approved member can sign in with an email address and password, is required to replace an initial password before using the application, and an administrator can add the small set of approved members without a public sign-up process.

The authentication identity must also be available to other modules so their records can identify who created or last modified them.

## Scope

**In scope**

- Email-and-password sign-in and sign-out for approved members.
- A protected administrator dashboard for creating users.
- A permanent administrator account with the email `ashwani@cuzorlabs.com`.
- First-sign-in password replacement and a basic password policy.
- Protection of application pages and actions from unauthenticated access.
- Baseline protections against common internet-based login attacks and an auditable record of authentication and user-management events.
- A stable user identity for use in `created-by` and `modified-by` attributes in future modules.

**Out of scope**

- Public registration or self-service sign-up.
- Social/third-party sign-in, multi-factor authentication, and passwordless sign-in.
- Self-service forgotten-password recovery.
- Detailed authorization roles beyond administrator and non-administrator member access.
- Retrofitting audit fields into modules that do not yet exist.

## Observable requirements

### R1 — Sign-in access
Approved users can sign in with their registered email address and password through a sign-in page. A successful sign-in starts an authenticated session; an unsuccessful sign-in does not start one.

**Acceptance:** Given an approved user whose password is valid, when the user submits that email address and password, then the user is signed in and can reach the application’s protected area.

### R2 — No public registration
Visitors cannot create their own accounts from the application.

**Acceptance:** Given an unauthenticated visitor, when the visitor looks for a way to join the application, then no sign-up or account-registration option is available.

### R3 — Protected application access
All application pages and actions other than those required to sign in are available only to an authenticated user. Signing out ends access to protected pages and actions.

**Acceptance:** Given a visitor with no authenticated session, when the visitor opens a protected page directly, then the visitor is sent to the sign-in page and the protected content is not shown.

### R4 — Administrator account
The account with email `ashwani@cuzorlabs.com` is an administrator. It cannot be deleted by any application user and remains an administrator.

**Acceptance:** Given an administrator viewing user management, when the administrator attempts to delete the `ashwani@cuzorlabs.com` account, then deletion is refused and the account remains present with administrator access.

### R5 — Administrator user creation
Only an authenticated administrator can access user management and create an approved user by supplying a valid, unused email address. User creation does not reveal or expose the user’s password after creation.

**Acceptance:** Given a signed-in administrator, when the administrator creates `member@example.test`, then that email is listed as an approved user and can be used to sign in.

### R6 — Initial password and mandatory replacement
A newly created user starts with the temporary password `Password@123`. On that user’s first successful sign-in, the user must choose a new compliant password before accessing any other protected page or action.

**Acceptance:** Given a newly created user, when the user signs in with `Password@123`, then the user is shown the password-change step and cannot enter the protected area until a compliant new password is saved.

### R7 — Password policy
A new or changed password must be at least six characters long and contain at least one letter, one digit, and one special character. The application explains a rejected password without exposing any stored password.

**Acceptance:** Given a user changing a password, when the user enters `abcdef`, then the application rejects it and states that a digit and special character are required; when the user enters `Abcde1!`, then the application accepts it.

### R8 — Unique, durable user identity
Each user has a stable internal user identifier in addition to the email address. The signed-in user’s identifier is available to application functions that create or modify business records, so those records can retain `created-by` and `modified-by` values.

**Acceptance:** Given a signed-in user who creates a record in a future module, when that record is saved, then its `created-by` value identifies that user and a later update by another signed-in user records that other user in `modified-by`.

### R9 — Secure handling of credentials and session
Passwords are never displayed after entry, included in logs, or stored in a recoverable form. The application uses a secure connection for sign-in and authenticated use. Sessions end on sign-out and after an approved period of inactivity.

**Acceptance:** Given a signed-in user who signs out, when the user uses the browser’s back button or reopens a protected address, then the protected content is not available unless the user signs in again.

### R10 — Resistance to login guessing
The sign-in process gives the same failure response for an unknown email address and an incorrect password. Repeated failed sign-in attempts for the same account or source are temporarily blocked before further attempts are accepted.

**Acceptance:** Given a sign-in attempt with an unknown email and another with a registered email plus an incorrect password, when each attempt fails, then the visible response does not say whether the email is registered; given attempts exceeding the approved limit, when another attempt is made during the block period, then sign-in is refused until the block period ends.

### R11 — Authentication and administration audit trail
The system records successful and unsuccessful sign-ins, sign-outs, password changes, temporary login blocks, and user creations with the event time and relevant user identity where known. Passwords and full credential values are never recorded.

**Acceptance:** Given an administrator creates a user and that user subsequently signs in, when an authorized reviewer checks the audit trail, then separate timestamped user-creation and successful-sign-in events are present and neither event contains a password.

## Business rules

1. Account email addresses are required and unique without regard to letter case; the same email cannot represent two users.
2. `ashwani@cuzorlabs.com` is the required administrator email. It is not deletable or demotable through the application.
3. Only administrators may create users or access user management. There is no public account creation.
4. The temporary password for every newly created account is exactly `Password@123`; it is valid only until the account completes the required first password change.
5. A compliant password has at least six characters, at least one letter, at least one digit, and at least one special character.
6. A user whose password change is required may sign in only to complete that change; all other protected functionality remains unavailable.
7. Authentication failures must not disclose whether an email address exists. Passwords must not appear in pages after submission, logs, audit records, or recoverable data.
8. The final approved values for inactivity timeout, failed-attempt limit, block duration, and audit-retention period must be applied consistently.
9. The user identifier used for future `created-by` and `modified-by` values is stable and is not dependent on a display value such as an email address.

## Open questions

1. How should the required `ashwani@cuzorlabs.com` administrator account receive its initial password, and must it also be forced to change that password on first sign-in? A secure deployment-only setup is needed; an initial administrator password should not be exposed in application content or source code.
2. What exact values should apply for session inactivity timeout, failed sign-in attempts before temporary block, and block duration? A simple proposed starting point is 30 minutes of inactivity, 5 failures, and a 15-minute block.
3. Should administrators be able to deactivate, delete, or reset passwords for non-administrator users? The request requires creation and protects the permanent administrator from deletion, but does not define management of other accounts.
4. Should the administrator email be immutable, or may its email address be changed while retaining its protected administrator identity?
5. Who may review the authentication audit trail, and how long must it be retained?
6. Is secure HTTPS termination provided by the deployment environment, or must the application itself enforce and redirect to secure connections?
