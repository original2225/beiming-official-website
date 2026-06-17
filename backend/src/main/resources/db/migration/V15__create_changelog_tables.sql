CREATE TABLE changelog_releases (
    id uuid PRIMARY KEY,
    release_id text NOT NULL UNIQUE,
    slug text NOT NULL UNIQUE,
    version_name text NOT NULL UNIQUE,
    title text NOT NULL,
    summary text NOT NULL,
    body text NOT NULL,
    type text NOT NULL CHECK (type IN ('SERVER_VERSION', 'PLUGIN_CHANGE', 'RULE_CHANGE', 'RESOURCE_PACK', 'MAP_UPDATE', 'MAINTENANCE', 'SECURITY', 'OTHER')),
    status text NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'NEEDS_CHANGES', 'PUBLISHED', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'MEMBER_ONLY', 'STAFF_ONLY')),
    impact_level text NOT NULL CHECK (impact_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    released_at timestamptz,
    effective_at timestamptz,
    minecraft_version text,
    plugin_versions jsonb NOT NULL DEFAULT '[]'::jsonb,
    resource_pack_versions jsonb NOT NULL DEFAULT '[]'::jsonb,
    map_version text,
    groups jsonb NOT NULL DEFAULT '[]'::jsonb,
    compatibility_notes text,
    known_issues text,
    rollback_notes text,
    security_public_summary text,
    internal_note text,
    related_resources jsonb NOT NULL DEFAULT '[]'::jsonb,
    related_server_instances jsonb NOT NULL DEFAULT '[]'::jsonb,
    related_content jsonb,
    calendar_sync_status text NOT NULL CHECK (calendar_sync_status IN ('SKIPPED', 'SYNCED', 'FAILED')),
    calendar_event_id text,
    calendar_synced_at timestamptz,
    notification_failure jsonb,
    bookmark_count integer NOT NULL DEFAULT 0 CHECK (bookmark_count >= 0),
    created_by text NOT NULL,
    updated_by text NOT NULL,
    reviewed_by text,
    review_comment text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    published_at timestamptz,
    offline_at timestamptz,
    archived_at timestamptz,
    deleted_at timestamptz
);

CREATE INDEX idx_changelog_releases_status ON changelog_releases(status);
CREATE INDEX idx_changelog_releases_released_at ON changelog_releases(released_at);
CREATE INDEX idx_changelog_releases_type ON changelog_releases(type);
CREATE INDEX idx_changelog_releases_created_by ON changelog_releases(created_by);

CREATE TABLE changelog_bookmarks (
    id uuid PRIMARY KEY,
    bookmark_id text NOT NULL UNIQUE,
    release_id text NOT NULL REFERENCES changelog_releases(release_id),
    user_id text NOT NULL,
    display_name_snapshot text NOT NULL,
    status text NOT NULL CHECK (status IN ('ACTIVE', 'CANCELED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    canceled_at timestamptz,
    CONSTRAINT uq_changelog_bookmark_user_release UNIQUE (release_id, user_id)
);

CREATE INDEX idx_changelog_bookmarks_release ON changelog_bookmarks(release_id);
CREATE INDEX idx_changelog_bookmarks_user ON changelog_bookmarks(user_id);
CREATE INDEX idx_changelog_bookmarks_status ON changelog_bookmarks(status);

CREATE TABLE changelog_calendar_syncs (
    id uuid PRIMARY KEY,
    request_id text NOT NULL UNIQUE,
    release_id text NOT NULL REFERENCES changelog_releases(release_id),
    sync_status text NOT NULL CHECK (sync_status IN ('SYNCED', 'SKIPPED', 'FAILED')),
    mode text NOT NULL CHECK (mode IN ('UPSERT_SNAPSHOT', 'DRY_RUN')),
    calendar_event_id text,
    items jsonb NOT NULL DEFAULT '[]'::jsonb,
    calendar_event_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    actor_user_id text NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_changelog_calendar_sync_release ON changelog_calendar_syncs(release_id);
CREATE INDEX idx_changelog_calendar_sync_status ON changelog_calendar_syncs(sync_status);
CREATE INDEX idx_changelog_calendar_sync_created_at ON changelog_calendar_syncs(created_at);
