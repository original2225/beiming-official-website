CREATE TABLE calendar_events (
    id uuid PRIMARY KEY,
    event_id text NOT NULL UNIQUE,
    source_type text NOT NULL CHECK (source_type IN ('MANUAL', 'ACTIVITY', 'CHANGELOG')),
    source_id text,
    source_version text,
    title text NOT NULL,
    summary text NOT NULL,
    description text NOT NULL,
    type text NOT NULL CHECK (type IN ('ACTIVITY', 'MAINTENANCE', 'ENGINEERING_MILESTONE', 'VOTE_DEADLINE', 'VERSION_RELEASE', 'SERVER_SCHEDULE')),
    status text NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'NEEDS_CHANGES', 'REJECTED', 'PUBLISHED', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'MEMBER_ONLY', 'STAFF_ONLY')),
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    timezone text NOT NULL,
    all_day boolean NOT NULL DEFAULT false,
    location_text text,
    related_url text,
    labels jsonb NOT NULL DEFAULT '[]'::jsonb,
    priority integer NOT NULL DEFAULT 50,
    watch_count integer NOT NULL DEFAULT 0 CHECK (watch_count >= 0),
    created_by text NOT NULL,
    updated_by text NOT NULL,
    reviewed_by text,
    reminder_failure jsonb,
    source_snapshot_stale boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    published_at timestamptz,
    offline_at timestamptz,
    archived_at timestamptz,
    deleted_at timestamptz,
    last_synced_at timestamptz,
    CONSTRAINT uq_calendar_source UNIQUE (source_type, source_id)
);

CREATE INDEX idx_calendar_events_status ON calendar_events(status);
CREATE INDEX idx_calendar_events_start_at ON calendar_events(start_at);
CREATE INDEX idx_calendar_events_source ON calendar_events(source_type, source_id);
CREATE INDEX idx_calendar_events_created_by ON calendar_events(created_by);

CREATE TABLE calendar_watches (
    id uuid PRIMARY KEY,
    watch_id text NOT NULL UNIQUE,
    event_id text NOT NULL REFERENCES calendar_events(event_id),
    user_id text NOT NULL,
    display_name_snapshot text NOT NULL,
    reminder_enabled boolean NOT NULL DEFAULT true,
    reminder_offsets jsonb NOT NULL DEFAULT '[]'::jsonb,
    status text NOT NULL CHECK (status IN ('ACTIVE', 'CANCELED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    canceled_at timestamptz,
    CONSTRAINT uq_calendar_watch_user_event UNIQUE (event_id, user_id)
);

CREATE INDEX idx_calendar_watches_event ON calendar_watches(event_id);
CREATE INDEX idx_calendar_watches_user ON calendar_watches(user_id);
CREATE INDEX idx_calendar_watches_status ON calendar_watches(status);

CREATE TABLE calendar_activity_sync_runs (
    id uuid PRIMARY KEY,
    request_id text NOT NULL UNIQUE,
    sync_status text NOT NULL CHECK (sync_status IN ('SYNCED', 'SKIPPED', 'FAILED')),
    mode text NOT NULL CHECK (mode IN ('UPSERT_SNAPSHOT', 'DRY_RUN')),
    range_from timestamptz NOT NULL,
    range_to timestamptz NOT NULL,
    created_total integer NOT NULL DEFAULT 0 CHECK (created_total >= 0),
    updated_total integer NOT NULL DEFAULT 0 CHECK (updated_total >= 0),
    skipped_total integer NOT NULL DEFAULT 0 CHECK (skipped_total >= 0),
    failed_total integer NOT NULL DEFAULT 0 CHECK (failed_total >= 0),
    actor_user_id text NOT NULL,
    activity_mode text NOT NULL,
    items jsonb NOT NULL DEFAULT '[]'::jsonb,
    synced_events jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_calendar_activity_sync_status ON calendar_activity_sync_runs(sync_status);
CREATE INDEX idx_calendar_activity_sync_created_at ON calendar_activity_sync_runs(created_at);
