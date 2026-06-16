CREATE TABLE onboarding_applications (
    id uuid PRIMARY KEY,
    application_id text NOT NULL UNIQUE,
    user_id text NOT NULL UNIQUE,
    display_name_snapshot text NOT NULL,
    auth_status_snapshot text NOT NULL DEFAULT 'ACTIVE',
    minecraft_binding_snapshot jsonb,
    status text NOT NULL CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'BLOCKED', 'READY_FOR_EXAM', 'WAITING_EXAM', 'READY_FOR_WHITELIST', 'WAITING_WHITELIST', 'COMPLETED', 'CANCELLED')),
    previous_status text,
    review_direction text CHECK (review_direction IS NULL OR review_direction IN ('REDSTONE', 'LATE_GAME', 'BUILDING', 'GENERAL')),
    profile_confirmation jsonb,
    rule_confirmation jsonb,
    blocked_reason text,
    blocked_by text,
    blocked_at timestamptz,
    notification_status text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    completed_at timestamptz,
    cancelled_at timestamptz
);

CREATE INDEX idx_onboarding_applications_status ON onboarding_applications(status);
CREATE INDEX idx_onboarding_applications_review_direction ON onboarding_applications(review_direction);
CREATE INDEX idx_onboarding_applications_updated_at ON onboarding_applications(updated_at);

CREATE TABLE onboarding_confirmations (
    id uuid PRIMARY KEY,
    confirmation_id text NOT NULL UNIQUE,
    application_id text NOT NULL REFERENCES onboarding_applications(application_id),
    confirmation_type text NOT NULL CHECK (confirmation_type IN ('PROFILE', 'RULES')),
    confirmation_payload jsonb NOT NULL,
    confirmed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE (application_id, confirmation_type)
);

CREATE INDEX idx_onboarding_confirmations_application_id ON onboarding_confirmations(application_id);

CREATE TABLE onboarding_state_events (
    id uuid PRIMARY KEY,
    event_id text NOT NULL UNIQUE,
    application_id text NOT NULL REFERENCES onboarding_applications(application_id),
    actor_user_id text NOT NULL,
    action text NOT NULL,
    before_status text,
    after_status text NOT NULL,
    reason text,
    event_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    request_id text NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_onboarding_state_events_application_id ON onboarding_state_events(application_id);
CREATE INDEX idx_onboarding_state_events_request_id ON onboarding_state_events(request_id);

CREATE TABLE onboarding_handoff_snapshots (
    id uuid PRIMARY KEY,
    handoff_id text NOT NULL UNIQUE,
    application_id text NOT NULL REFERENCES onboarding_applications(application_id),
    target_module text NOT NULL CHECK (target_module IN ('EXAM', 'WHITELIST')),
    handoff_version integer NOT NULL CHECK (handoff_version > 0),
    snapshot_payload jsonb NOT NULL,
    generated_by text NOT NULL,
    request_id text NOT NULL,
    generated_at timestamptz NOT NULL
);

CREATE INDEX idx_onboarding_handoff_snapshots_application_id ON onboarding_handoff_snapshots(application_id);
