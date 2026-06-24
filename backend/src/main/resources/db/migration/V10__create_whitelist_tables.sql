CREATE TABLE whitelist_applications (
    id uuid PRIMARY KEY,
    application_id text NOT NULL UNIQUE,
    exam_session_id text NOT NULL,
    onboarding_application_id text NOT NULL,
    exam_handoff_version integer NOT NULL CHECK (exam_handoff_version > 0),
    onboarding_handoff_version integer NOT NULL CHECK (onboarding_handoff_version > 0),
    user_id text NOT NULL,
    display_name_snapshot text NOT NULL,
    minecraft_binding_snapshot jsonb NOT NULL,
    review_direction text NOT NULL CHECK (review_direction IN ('REDSTONE', 'LATE_GAME', 'BUILDING', 'GENERAL')),
    attempt_type text NOT NULL CHECK (attempt_type IN ('FIRST_TIME', 'RECHECK')),
    status text NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'UNDER_REVIEW', 'NEEDS_SUPPLEMENT', 'SUPPLEMENT_SUBMITTED', 'APPROVAL_BLOCKED', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'REMOVED', 'REAPPLYING', 'ARCHIVED')),
    result text NOT NULL CHECK (result IN ('PENDING', 'NEEDS_SUPPLEMENT', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'REMOVED')),
    materials_payload jsonb NOT NULL DEFAULT '[]'::jsonb,
    score_summary jsonb,
    exam_passed_at timestamptz,
    reviewer_user_id text,
    reviewer_display_name_snapshot text,
    review_comment text,
    internal_note text,
    supplement_request jsonb,
    profile_activation jsonb,
    attendance_handoff jsonb,
    notification_status text,
    notification_failure jsonb,
    removed_at timestamptz,
    removed_by text,
    removal_reason text,
    reapply_required boolean NOT NULL DEFAULT false,
    next_exam_attempt_type text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    approved_at timestamptz,
    rejected_at timestamptz,
    withdrawn_at timestamptz,
    archived_at timestamptz
);

CREATE INDEX idx_whitelist_applications_user_id ON whitelist_applications(user_id);
CREATE INDEX idx_whitelist_applications_status ON whitelist_applications(status);
CREATE INDEX idx_whitelist_applications_result ON whitelist_applications(result);
CREATE INDEX idx_whitelist_applications_updated_at ON whitelist_applications(updated_at);

CREATE TABLE whitelist_state_events (
    id uuid PRIMARY KEY,
    event_id text NOT NULL UNIQUE,
    application_id text NOT NULL REFERENCES whitelist_applications(application_id),
    actor_user_id text NOT NULL,
    action text NOT NULL,
    before_status text,
    after_status text NOT NULL,
    reason text,
    event_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    request_id text NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_whitelist_state_events_application_id ON whitelist_state_events(application_id);
CREATE INDEX idx_whitelist_state_events_request_id ON whitelist_state_events(request_id);

CREATE TABLE whitelist_profile_activations (
    id uuid PRIMARY KEY,
    activation_id text NOT NULL UNIQUE,
    application_id text NOT NULL REFERENCES whitelist_applications(application_id),
    member_id text,
    status text NOT NULL CHECK (status IN ('ACTIVATED', 'FAILED')),
    activation_payload jsonb NOT NULL,
    request_id text NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_whitelist_profile_activations_application_id ON whitelist_profile_activations(application_id);

CREATE TABLE whitelist_attendance_handoffs (
    id uuid PRIMARY KEY,
    handoff_id text NOT NULL UNIQUE,
    application_id text NOT NULL REFERENCES whitelist_applications(application_id),
    member_id text NOT NULL,
    initialization_status text NOT NULL CHECK (initialization_status IN ('WAITING_MODULE', 'CONSUMED', 'FAILED')),
    handoff_version integer NOT NULL CHECK (handoff_version > 0),
    handoff_payload jsonb NOT NULL,
    generated_by text NOT NULL,
    request_id text NOT NULL,
    generated_at timestamptz NOT NULL,
    consumed_at timestamptz
);

CREATE INDEX idx_whitelist_attendance_handoffs_application_id ON whitelist_attendance_handoffs(application_id);
