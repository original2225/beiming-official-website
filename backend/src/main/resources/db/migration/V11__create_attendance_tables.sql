CREATE TABLE attendance_accounts (
    id uuid PRIMARY KEY,
    account_id text NOT NULL UNIQUE,
    user_id text NOT NULL,
    member_id text NOT NULL UNIQUE,
    display_name_snapshot text NOT NULL,
    avatar_url_snapshot text,
    member_group_snapshot text,
    member_status_snapshot text,
    minecraft_binding_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    status text NOT NULL CHECK (status IN ('PENDING_INITIALIZATION', 'ACTIVE', 'FROZEN', 'REMOVAL_CANDIDATE', 'REMOVED', 'ARCHIVED')),
    score_balance integer NOT NULL CHECK (score_balance >= 0),
    initial_score integer NOT NULL CHECK (initial_score >= 0),
    total_earned integer NOT NULL CHECK (total_earned >= 0),
    total_deducted integer NOT NULL CHECK (total_deducted >= 0),
    last_positive_activity_at timestamptz,
    last_deducted_at timestamptz,
    last_ledger_id text,
    whitelist_application_id text NOT NULL UNIQUE,
    whitelist_handoff_id text NOT NULL,
    whitelist_handoff_version integer NOT NULL CHECK (whitelist_handoff_version > 0),
    review_direction text NOT NULL,
    attempt_type text NOT NULL CHECK (attempt_type IN ('FIRST_TIME', 'RECHECK')),
    notification_status text,
    notification_failure jsonb,
    profile_snapshot_stale boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE INDEX idx_attendance_accounts_user_id ON attendance_accounts(user_id);
CREATE INDEX idx_attendance_accounts_status ON attendance_accounts(status);
CREATE INDEX idx_attendance_accounts_score ON attendance_accounts(score_balance);
CREATE INDEX idx_attendance_accounts_updated_at ON attendance_accounts(updated_at);

CREATE TABLE attendance_ledgers (
    id uuid PRIMARY KEY,
    ledger_id text NOT NULL UNIQUE,
    account_id text NOT NULL REFERENCES attendance_accounts(account_id),
    member_id text NOT NULL,
    user_id text NOT NULL,
    type text NOT NULL CHECK (type IN ('INITIAL_GRANT', 'ADMIN_ADJUSTMENT', 'ACTIVITY_REWARD', 'CONTRIBUTION_REWARD', 'MONTHLY_DEDUCTION', 'REVERSAL')),
    status text NOT NULL CHECK (status IN ('POSTED', 'REVERSED')),
    delta integer NOT NULL,
    balance_before integer NOT NULL CHECK (balance_before >= 0),
    balance_after integer NOT NULL CHECK (balance_after >= 0),
    source_module text NOT NULL,
    source_id text NOT NULL,
    cycle_key text,
    reason text,
    public_reason text,
    operator_user_id text NOT NULL,
    idempotency_key text,
    reversal_of_ledger_id text,
    reversed_by_ledger_id text,
    notification_status text,
    notification_failure jsonb,
    created_at timestamptz NOT NULL,
    reversed_at timestamptz
);

CREATE INDEX idx_attendance_ledgers_account_id ON attendance_ledgers(account_id);
CREATE INDEX idx_attendance_ledgers_type ON attendance_ledgers(type);
CREATE INDEX idx_attendance_ledgers_source ON attendance_ledgers(source_module, source_id);
CREATE INDEX idx_attendance_ledgers_cycle_key ON attendance_ledgers(cycle_key);

CREATE TABLE attendance_contributions (
    id uuid PRIMARY KEY,
    contribution_id text NOT NULL UNIQUE,
    account_id text NOT NULL REFERENCES attendance_accounts(account_id),
    member_id text NOT NULL,
    user_id text NOT NULL,
    type text NOT NULL CHECK (type IN ('ONLINE_ACTIVE', 'PROJECT_BUILD', 'EVENT_PARTICIPATION', 'WORK_SUBMISSION', 'HELPER_SUPPORT', 'MANUAL')),
    source_module text NOT NULL,
    source_id text NOT NULL,
    title text NOT NULL,
    description text,
    occurred_at timestamptz NOT NULL,
    score_delta integer NOT NULL CHECK (score_delta >= 0),
    ledger_id text,
    operator_user_id text NOT NULL,
    correction_of_contribution_id text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (account_id, source_module, source_id)
);

CREATE INDEX idx_attendance_contributions_account_id ON attendance_contributions(account_id);
CREATE INDEX idx_attendance_contributions_type ON attendance_contributions(type);
CREATE INDEX idx_attendance_contributions_occurred_at ON attendance_contributions(occurred_at);

CREATE TABLE attendance_monthly_runs (
    id uuid PRIMARY KEY,
    run_id text NOT NULL UNIQUE,
    cycle_key text NOT NULL,
    status text NOT NULL CHECK (status IN ('PREVIEW', 'COMPLETED', 'FAILED')),
    dry_run boolean NOT NULL DEFAULT false,
    reason text NOT NULL,
    deduction_score integer NOT NULL CHECK (deduction_score > 0),
    eligible_accounts integer NOT NULL CHECK (eligible_accounts >= 0),
    deducted_accounts integer NOT NULL CHECK (deducted_accounts >= 0),
    skipped_accounts integer NOT NULL CHECK (skipped_accounts >= 0),
    candidate_created integer NOT NULL CHECK (candidate_created >= 0),
    idempotency_key text,
    started_at timestamptz,
    completed_at timestamptz,
    failure_reason text,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE (cycle_key, dry_run)
);

CREATE INDEX idx_attendance_monthly_runs_cycle_key ON attendance_monthly_runs(cycle_key);
CREATE INDEX idx_attendance_monthly_runs_status ON attendance_monthly_runs(status);

CREATE TABLE attendance_removal_candidates (
    id uuid PRIMARY KEY,
    candidate_id text NOT NULL UNIQUE,
    account_id text NOT NULL REFERENCES attendance_accounts(account_id),
    member_id text NOT NULL,
    user_id text NOT NULL,
    display_name_snapshot text NOT NULL,
    score_balance integer NOT NULL CHECK (score_balance >= 0),
    cycle_key text,
    status text NOT NULL CHECK (status IN ('OPEN', 'CONFIRMED', 'DISMISSED', 'EXPIRED')),
    reason text,
    public_reason text,
    recommended_action text NOT NULL,
    confirmed_by text,
    confirmed_at timestamptz,
    dismissed_by text,
    dismissed_at timestamptz,
    dismiss_reason text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_attendance_candidates_account_id ON attendance_removal_candidates(account_id);
CREATE INDEX idx_attendance_candidates_status ON attendance_removal_candidates(status);
CREATE INDEX idx_attendance_candidates_cycle_key ON attendance_removal_candidates(cycle_key);

CREATE TABLE attendance_leaderboard_snapshots (
    id uuid PRIMARY KEY,
    snapshot_id text NOT NULL UNIQUE,
    cycle_key text,
    entries_payload jsonb NOT NULL DEFAULT '[]'::jsonb,
    rebuilt_by text NOT NULL,
    request_id text NOT NULL,
    rebuilt_at timestamptz NOT NULL
);

CREATE INDEX idx_attendance_leaderboard_cycle_key ON attendance_leaderboard_snapshots(cycle_key);
