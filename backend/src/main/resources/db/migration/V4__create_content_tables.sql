CREATE TABLE content_categories (
    id uuid PRIMARY KEY,
    category_id text NOT NULL UNIQUE,
    name text NOT NULL,
    slug text NOT NULL UNIQUE,
    description text,
    sort_order integer NOT NULL DEFAULT 0,
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_content_categories_archived ON content_categories(archived);

CREATE TABLE content_tags (
    id uuid PRIMARY KEY,
    tag_id text NOT NULL UNIQUE,
    name text NOT NULL,
    slug text NOT NULL UNIQUE,
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_content_tags_archived ON content_tags(archived);

CREATE TABLE content_items (
    id uuid PRIMARY KEY,
    content_id text NOT NULL UNIQUE,
    type text NOT NULL CHECK (type IN ('ARTICLE', 'ANNOUNCEMENT', 'PAGE', 'PHOTO', 'MEMBER_WORK', 'SERVER_PROGRESS', 'MOMENT', 'PROGRESS', 'ACHIEVEMENT', 'MILESTONE', 'TOPIC_ENTRY')),
    status text NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'NEEDS_CHANGES', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'MEMBER_ONLY', 'PRIVATE')),
    slug text NOT NULL UNIQUE,
    title text NOT NULL,
    summary text,
    body text NOT NULL,
    cover_url text,
    category_id text REFERENCES content_categories(category_id),
    author_user_id text,
    author_display_name_snapshot text,
    member_snapshot jsonb,
    seo jsonb,
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

CREATE INDEX idx_content_items_status_visibility ON content_items(status, visibility);
CREATE INDEX idx_content_items_category_id ON content_items(category_id);
CREATE INDEX idx_content_items_published_at ON content_items(published_at);

CREATE TABLE content_item_tags (
    id uuid PRIMARY KEY,
    content_id text NOT NULL REFERENCES content_items(content_id),
    tag_id text NOT NULL REFERENCES content_tags(tag_id),
    created_at timestamptz NOT NULL,
    UNIQUE (content_id, tag_id)
);

CREATE INDEX idx_content_item_tags_tag_id ON content_item_tags(tag_id);

CREATE TABLE content_item_versions (
    id uuid PRIMARY KEY,
    content_id text NOT NULL REFERENCES content_items(content_id),
    version integer NOT NULL CHECK (version > 0),
    source_action text NOT NULL,
    snapshot jsonb NOT NULL,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL,
    reason text,
    restored_from_version integer,
    UNIQUE (content_id, version)
);

CREATE INDEX idx_content_item_versions_content_id ON content_item_versions(content_id);

CREATE TABLE content_topics (
    id uuid PRIMARY KEY,
    topic_id text NOT NULL UNIQUE,
    slug text NOT NULL UNIQUE,
    title text NOT NULL,
    summary text,
    cover_url text,
    status text NOT NULL CHECK (status IN ('DRAFT', 'APPROVED', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'MEMBER_ONLY', 'PRIVATE')),
    seo jsonb,
    published_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    deleted_at timestamptz
);

CREATE INDEX idx_content_topics_status_visibility ON content_topics(status, visibility);

CREATE TABLE content_topic_items (
    id uuid PRIMARY KEY,
    topic_id text NOT NULL REFERENCES content_topics(topic_id),
    content_id text NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    UNIQUE (topic_id, content_id)
);

CREATE INDEX idx_content_topic_items_topic_id ON content_topic_items(topic_id);

CREATE TABLE content_home_configs (
    id uuid PRIMARY KEY,
    home_config_id text NOT NULL UNIQUE,
    version integer NOT NULL,
    sections jsonb NOT NULL DEFAULT '[]'::jsonb,
    seo jsonb,
    published boolean NOT NULL DEFAULT false,
    published_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_content_home_configs_published ON content_home_configs(published);

CREATE TABLE content_home_versions (
    id uuid PRIMARY KEY,
    home_config_id text NOT NULL,
    version integer NOT NULL UNIQUE,
    sections jsonb NOT NULL DEFAULT '[]'::jsonb,
    seo jsonb,
    published_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE TABLE content_preview_tokens (
    id uuid PRIMARY KEY,
    content_id text NOT NULL REFERENCES content_items(content_id),
    token_hash text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_content_preview_tokens_content_id ON content_preview_tokens(content_id);

CREATE TABLE content_seo_configs (
    id uuid PRIMARY KEY,
    seo_id text NOT NULL UNIQUE,
    route text NOT NULL UNIQUE,
    title text NOT NULL,
    description text,
    keywords jsonb NOT NULL DEFAULT '[]'::jsonb,
    cover_url text,
    robots text NOT NULL CHECK (robots IN ('INDEX_FOLLOW', 'NOINDEX_FOLLOW', 'NOINDEX_NOFOLLOW')),
    canonical_url text,
    enabled boolean NOT NULL DEFAULT true,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_content_seo_configs_enabled ON content_seo_configs(enabled);
