PRAGMA foreign_keys = ON;
CREATE TABLE users (
 id TEXT PRIMARY KEY, email TEXT NOT NULL COLLATE NOCASE UNIQUE,
 verifier_algorithm TEXT NOT NULL, verifier_work_factor INTEGER NOT NULL, verifier_salt BLOB NOT NULL, verifier_digest BLOB NOT NULL,
 is_administrator INTEGER NOT NULL, must_change_password INTEGER NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);
CREATE TABLE auth_sessions (
 token_digest TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id), csrf_digest TEXT NOT NULL,
 issued_at TEXT NOT NULL, last_seen_at TEXT NOT NULL, revoked_at TEXT
);
CREATE TABLE login_throttles (
 scope TEXT NOT NULL, subject TEXT NOT NULL, window_started_at TEXT NOT NULL, failure_count INTEGER NOT NULL, block_until TEXT,
 PRIMARY KEY(scope, subject)
);
CREATE TABLE auth_audit_events (
 id TEXT PRIMARY KEY, event_type TEXT NOT NULL, occurred_at TEXT NOT NULL, actor_id TEXT, subject_id TEXT, source_fingerprint TEXT
);
CREATE INDEX auth_audit_events_occurred_at_idx ON auth_audit_events(occurred_at);
CREATE INDEX auth_audit_events_subject_id_idx ON auth_audit_events(subject_id);
CREATE TRIGGER permanent_admin_no_delete BEFORE DELETE ON users WHEN OLD.email = 'ashwani@cuzorlabs.com' BEGIN SELECT RAISE(ABORT, 'permanent administrator cannot be deleted'); END;
CREATE TRIGGER permanent_admin_no_email_change BEFORE UPDATE OF email ON users WHEN OLD.email = 'ashwani@cuzorlabs.com' AND NEW.email <> OLD.email BEGIN SELECT RAISE(ABORT, 'permanent administrator email cannot change'); END;
CREATE TRIGGER permanent_admin_no_demotion BEFORE UPDATE OF is_administrator ON users WHEN OLD.email = 'ashwani@cuzorlabs.com' AND NEW.is_administrator = 0 BEGIN SELECT RAISE(ABORT, 'permanent administrator cannot be demoted'); END;
