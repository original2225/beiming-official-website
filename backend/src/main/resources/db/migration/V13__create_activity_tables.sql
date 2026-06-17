CREATE TABLE activity_events (
    id uuid PRIMARY KEY,
    activity_id text NOT NULL UNIQUE,
    slug text NOT NULL UNIQUE,
    title text NOT NULL,
    summary text NOT NULL,
    description text NOT NULL,
    type text NOT NULL CHECK (type IN ('BUILD', 'PVP', 'PVE', 'COMMUNITY', 'CONTEST', 'MAINTENANCE', 'OTHER')),
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'MEMBER_ONLY', 'STAFF_ONLY', 'INVITE_ONLY')),
    registration_policy text NOT NULL CHECK (registration_policy IN ('OPEN', 'APPROVAL_REQUIRED', 'INVITE_ONLY', 'CLOSED')),
    status text NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'NEEDS_CHANGES', 'REJECTED', 'PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED', 'RUNNING', 'COMPLETED', 'RESULT_PUBLISHED', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    registration_open_at timestamptz,
    registration_close_at timestamptz,
    capacity integer NOT NULL CHECK (capacity >= 1),
    waitlist_capacity integer NOT NULL CHECK (waitlist_capacity >= 0),
    confirmed_count integer NOT NULL DEFAULT 0 CHECK (confirmed_count >= 0),
    waitlisted_count integer NOT NULL DEFAULT 0 CHECK (waitlisted_count >= 0),
    checked_in_count integer NOT NULL DEFAULT 0 CHECK (checked_in_count >= 0),
    no_show_count integer NOT NULL DEFAULT 0 CHECK (no_show_count >= 0),
    location_text text,
    cover_image_url text,
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_by text NOT NULL,
    notification_failure jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    published_at timestamptz,
    offline_at timestamptz,
    archived_at timestamptz,
    deleted_at timestamptz
);

CREATE INDEX idx_activity_events_status ON activity_events(status);
CREATE INDEX idx_activity_events_start_at ON activity_events(start_at);
CREATE INDEX idx_activity_events_created_by ON activity_events(created_by);

CREATE TABLE activity_registrations (
    id uuid PRIMARY KEY,
    registration_id text NOT NULL UNIQUE,
    activity_id text NOT NULL REFERENCES activity_events(activity_id),
    user_id text NOT NULL,
    member_id text,
    participant_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    status text NOT NULL CHECK (status IN ('SUBMITTED', 'CONFIRMED', 'WAITLISTED', 'REJECTED', 'CANCELED', 'CHECKED_IN', 'NO_SHOW')),
    answers jsonb NOT NULL DEFAULT '{}'::jsonb,
    guest_count integer NOT NULL DEFAULT 0 CHECK (guest_count >= 0),
    waitlist_rank integer,
    notification_failure jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    checked_in_at timestamptz,
    no_show_at timestamptz,
    canceled_at timestamptz
);

CREATE INDEX idx_activity_registrations_activity ON activity_registrations(activity_id);
CREATE INDEX idx_activity_registrations_user ON activity_registrations(user_id);
CREATE INDEX idx_activity_registrations_status ON activity_registrations(status);

CREATE TABLE activity_results (
    id uuid PRIMARY KEY,
    result_id text NOT NULL UNIQUE,
    activity_id text NOT NULL UNIQUE REFERENCES activity_events(activity_id),
    status text NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    title text NOT NULL,
    summary text NOT NULL,
    details text NOT NULL,
    participant_total integer NOT NULL DEFAULT 0 CHECK (participant_total >= 0),
    winner_total integer NOT NULL DEFAULT 0 CHECK (winner_total >= 0),
    published_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_activity_results_status ON activity_results(status);

CREATE TABLE activity_rewards (
    id uuid PRIMARY KEY,
    reward_id text NOT NULL UNIQUE,
    activity_id text NOT NULL REFERENCES activity_events(activity_id),
    registration_id text NOT NULL REFERENCES activity_registrations(registration_id),
    user_id text NOT NULL,
    member_id text,
    recipient_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    type text NOT NULL CHECK (type IN ('POINTS_CANDIDATE', 'ITEM', 'TITLE', 'BADGE', 'OTHER')),
    title text NOT NULL,
    description text,
    quantity integer NOT NULL CHECK (quantity > 0),
    score_candidate_delta integer NOT NULL DEFAULT 0,
    status text NOT NULL CHECK (status IN ('PENDING_ISSUE', 'ISSUED', 'REVOKED', 'ARCHIVED')),
    notification_failure jsonb,
    issued_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_activity_rewards_activity ON activity_rewards(activity_id);
CREATE INDEX idx_activity_rewards_user ON activity_rewards(user_id);
CREATE INDEX idx_activity_rewards_status ON activity_rewards(status);

CREATE TABLE activity_contribution_candidates (
    id uuid PRIMARY KEY,
    candidate_id text NOT NULL UNIQUE,
    activity_id text NOT NULL REFERENCES activity_events(activity_id),
    reward_id text NOT NULL REFERENCES activity_rewards(reward_id),
    member_id text,
    user_id text NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    score_delta integer NOT NULL,
    status text NOT NULL CHECK (status IN ('PENDING', 'HANDED_OFF', 'FAILED', 'ARCHIVED')),
    attendance_response_summary jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_activity_candidates_activity ON activity_contribution_candidates(activity_id);
CREATE INDEX idx_activity_candidates_reward ON activity_contribution_candidates(reward_id);
CREATE INDEX idx_activity_candidates_status ON activity_contribution_candidates(status);
