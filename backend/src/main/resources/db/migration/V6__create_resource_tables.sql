CREATE TABLE resource_categories (
    id uuid PRIMARY KEY,
    category_id text NOT NULL UNIQUE,
    name text NOT NULL,
    slug text NOT NULL UNIQUE,
    description text,
    icon text,
    sort_order integer NOT NULL DEFAULT 100,
    enabled boolean NOT NULL DEFAULT true,
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE INDEX idx_resource_categories_enabled_archived ON resource_categories(enabled, archived);

CREATE TABLE resource_items (
    id uuid PRIMARY KEY,
    resource_id text NOT NULL UNIQUE,
    status text NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'NEEDS_CHANGES', 'PUBLISHED', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    type text NOT NULL CHECK (type IN ('CLIENT_PACK', 'RESOURCE_PACK', 'SHADER_PACK', 'MAP_FILE', 'RULE_DOCUMENT', 'ACTIVITY_RESOURCE', 'GUIDE_ATTACHMENT', 'OTHER')),
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'AUTHENTICATED', 'MEMBER_ONLY', 'ADMIN_ONLY')),
    slug text NOT NULL UNIQUE,
    title text NOT NULL,
    summary text,
    description text,
    cover_url text,
    category_id text REFERENCES resource_categories(category_id),
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    maintainer_member_id text,
    maintainer_snapshot jsonb,
    admin_note text,
    review_opinion text,
    notification_status text,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    published_at timestamptz,
    visible_from timestamptz,
    visible_until timestamptz,
    created_by text NOT NULL,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    deleted_at timestamptz
);

CREATE INDEX idx_resource_items_status_visibility ON resource_items(status, visibility);
CREATE INDEX idx_resource_items_category_id ON resource_items(category_id);
CREATE INDEX idx_resource_items_published_at ON resource_items(published_at);

CREATE TABLE resource_versions (
    id uuid PRIMARY KEY,
    version_id text NOT NULL UNIQUE,
    resource_id text NOT NULL REFERENCES resource_items(resource_id),
    status text NOT NULL CHECK (status IN ('ENABLED', 'DISABLED', 'ARCHIVED')),
    version_name text NOT NULL,
    title text,
    changelog text,
    minecraft_versions jsonb NOT NULL DEFAULT '[]'::jsonb,
    loader text,
    file_size_bytes bigint,
    checksum_sha256 text,
    released_at timestamptz,
    created_by text NOT NULL,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (resource_id, version_name)
);

CREATE INDEX idx_resource_versions_resource_id ON resource_versions(resource_id);
CREATE INDEX idx_resource_versions_status ON resource_versions(status);

CREATE TABLE resource_download_entries (
    id uuid PRIMARY KEY,
    download_entry_id text NOT NULL UNIQUE,
    resource_id text NOT NULL REFERENCES resource_items(resource_id),
    version_id text NOT NULL REFERENCES resource_versions(version_id),
    provider text NOT NULL CHECK (provider IN ('CLOUDREVE_SHARE', 'EXTERNAL_URL')),
    status text NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED', 'UNAVAILABLE')),
    display_name text,
    share_url text,
    last_checked_at timestamptz,
    expires_at timestamptz,
    admin_note text,
    cloud_mode text,
    password_required boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_resource_download_entries_version_id ON resource_download_entries(version_id);
CREATE INDEX idx_resource_download_entries_status ON resource_download_entries(status);

CREATE TABLE resource_download_records (
    id uuid PRIMARY KEY,
    ticket_id text NOT NULL UNIQUE,
    resource_id text NOT NULL,
    version_id text NOT NULL,
    download_entry_id text NOT NULL,
    actor_user_id text,
    anonymous boolean NOT NULL DEFAULT false,
    client_label text,
    provider text NOT NULL,
    result text NOT NULL CHECK (result IN ('SUCCESS', 'DEGRADED', 'FAILED')),
    degraded boolean NOT NULL DEFAULT false,
    request_id text NOT NULL,
    created_at timestamptz NOT NULL,
    ticket_summary jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_resource_download_records_resource_id ON resource_download_records(resource_id);
CREATE INDEX idx_resource_download_records_request_id ON resource_download_records(request_id);
