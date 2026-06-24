CREATE TABLE exam_sessions (
    id uuid PRIMARY KEY,
    session_id text NOT NULL UNIQUE,
    application_id text NOT NULL,
    onboarding_handoff_version integer NOT NULL CHECK (onboarding_handoff_version > 0),
    user_id text NOT NULL,
    display_name_snapshot text NOT NULL,
    minecraft_binding_snapshot jsonb,
    review_direction text NOT NULL CHECK (review_direction IN ('REDSTONE', 'LATE_GAME', 'BUILDING', 'GENERAL')),
    attempt_type text NOT NULL CHECK (attempt_type IN ('FIRST_TIME', 'RECHECK')),
    difficulty text NOT NULL CHECK (difficulty IN ('NORMAL', 'RECHECK')),
    status text NOT NULL CHECK (status IN ('IN_PROGRESS', 'PENDING_MANUAL_REVIEW', 'NEEDS_SUPPLEMENT', 'SUPPLEMENT_SUBMITTED', 'AUTO_PASSED', 'AUTO_FAILED', 'MANUAL_PASSED', 'MANUAL_FAILED', 'EXPIRED', 'CANCELLED')),
    result text NOT NULL CHECK (result IN ('PENDING', 'NEEDS_SUPPLEMENT', 'PASSED', 'FAILED', 'CANCELLED')),
    template_id text NOT NULL,
    template_version integer NOT NULL CHECK (template_version > 0),
    paper_id text NOT NULL,
    paper_snapshot jsonb NOT NULL,
    answer_snapshot jsonb NOT NULL DEFAULT '[]'::jsonb,
    score_summary jsonb,
    manual_review jsonb,
    supplement_request jsonb,
    notification_status text,
    started_at timestamptz,
    last_saved_at timestamptz,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    expires_at timestamptz,
    passed_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_exam_sessions_user_id ON exam_sessions(user_id);
CREATE INDEX idx_exam_sessions_status ON exam_sessions(status);
CREATE INDEX idx_exam_sessions_result ON exam_sessions(result);
CREATE INDEX idx_exam_sessions_updated_at ON exam_sessions(updated_at);

CREATE TABLE exam_answer_sheets (
    id uuid PRIMARY KEY,
    answer_sheet_id text NOT NULL UNIQUE,
    session_id text NOT NULL REFERENCES exam_sessions(session_id),
    actor_user_id text NOT NULL,
    draft boolean NOT NULL,
    answers_payload jsonb NOT NULL,
    request_id text NOT NULL,
    saved_at timestamptz NOT NULL
);

CREATE INDEX idx_exam_answer_sheets_session_id ON exam_answer_sheets(session_id);
CREATE INDEX idx_exam_answer_sheets_request_id ON exam_answer_sheets(request_id);

CREATE TABLE exam_reviews (
    id uuid PRIMARY KEY,
    review_id text NOT NULL UNIQUE,
    session_id text NOT NULL REFERENCES exam_sessions(session_id),
    reviewer_user_id text NOT NULL,
    result text NOT NULL CHECK (result IN ('PASSED', 'FAILED')),
    review_payload jsonb NOT NULL,
    request_id text NOT NULL,
    reviewed_at timestamptz NOT NULL
);

CREATE INDEX idx_exam_reviews_session_id ON exam_reviews(session_id);
CREATE INDEX idx_exam_reviews_request_id ON exam_reviews(request_id);

CREATE TABLE exam_questions (
    id uuid PRIMARY KEY,
    question_id text NOT NULL UNIQUE,
    version integer NOT NULL CHECK (version > 0),
    type text NOT NULL CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_TEXT')),
    review_direction text NOT NULL CHECK (review_direction IN ('REDSTONE', 'LATE_GAME', 'BUILDING', 'GENERAL')),
    difficulty text NOT NULL CHECK (difficulty IN ('NORMAL', 'RECHECK')),
    stem text NOT NULL,
    options jsonb NOT NULL DEFAULT '[]'::jsonb,
    correct_option_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    reference_answer text,
    score integer NOT NULL CHECK (score > 0),
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    status text NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE INDEX idx_exam_questions_status ON exam_questions(status);
CREATE INDEX idx_exam_questions_direction ON exam_questions(review_direction);

CREATE TABLE exam_question_versions (
    id uuid PRIMARY KEY,
    question_id text NOT NULL,
    version integer NOT NULL CHECK (version > 0),
    question_snapshot jsonb NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE (question_id, version)
);

CREATE INDEX idx_exam_question_versions_question_id ON exam_question_versions(question_id);

CREATE TABLE exam_paper_templates (
    id uuid PRIMARY KEY,
    template_id text NOT NULL UNIQUE,
    version integer NOT NULL CHECK (version > 0),
    name text NOT NULL,
    review_direction text NOT NULL CHECK (review_direction IN ('REDSTONE', 'LATE_GAME', 'BUILDING', 'GENERAL')),
    difficulty text NOT NULL CHECK (difficulty IN ('NORMAL', 'RECHECK')),
    status text NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    time_limit_minutes integer NOT NULL CHECK (time_limit_minutes > 0),
    pass_score integer NOT NULL CHECK (pass_score > 0),
    objective_pass_score integer NOT NULL CHECK (objective_pass_score >= 0),
    question_rules jsonb NOT NULL,
    content_rule_version text,
    retake_cooldown_hours integer NOT NULL CHECK (retake_cooldown_hours >= 0),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    published_at timestamptz
);

CREATE INDEX idx_exam_paper_templates_status ON exam_paper_templates(status);
CREATE INDEX idx_exam_paper_templates_direction ON exam_paper_templates(review_direction);

CREATE TABLE exam_handoff_snapshots (
    id uuid PRIMARY KEY,
    handoff_id text NOT NULL UNIQUE,
    session_id text NOT NULL REFERENCES exam_sessions(session_id),
    target_module text NOT NULL CHECK (target_module IN ('WHITELIST')),
    handoff_version integer NOT NULL CHECK (handoff_version > 0),
    snapshot_payload jsonb NOT NULL,
    generated_by text NOT NULL,
    request_id text NOT NULL,
    generated_at timestamptz NOT NULL
);

CREATE INDEX idx_exam_handoff_snapshots_session_id ON exam_handoff_snapshots(session_id);
