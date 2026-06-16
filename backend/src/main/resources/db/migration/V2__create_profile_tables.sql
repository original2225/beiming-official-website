CREATE TABLE profile_member_groups (
    id uuid PRIMARY KEY,
    group_id text NOT NULL UNIQUE,
    name text NOT NULL,
    name_normalized text NOT NULL UNIQUE,
    description text,
    color text,
    sort_order integer NOT NULL DEFAULT 0,
    archived boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE TABLE profile_members (
    id uuid PRIMARY KEY,
    member_id text NOT NULL UNIQUE,
    user_id text NOT NULL UNIQUE,
    display_name_snapshot text NOT NULL,
    auth_user_status_snapshot text NOT NULL,
    auth_roles_snapshot jsonb NOT NULL DEFAULT '[]'::jsonb,
    avatar_url text,
    minecraft_id text,
    minecraft_uuid text UNIQUE,
    skin_url text,
    group_id text REFERENCES profile_member_groups(group_id),
    status text NOT NULL CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'REMOVED', 'ARCHIVED')),
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    joined_at timestamptz NOT NULL,
    bio text,
    admin_note text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE INDEX idx_profile_members_user_id ON profile_members(user_id);
CREATE INDEX idx_profile_members_group_id ON profile_members(group_id);
CREATE INDEX idx_profile_members_status_visibility ON profile_members(status, visibility);

CREATE TABLE profile_member_milestones (
    id uuid PRIMARY KEY,
    milestone_id text NOT NULL UNIQUE,
    member_id text NOT NULL REFERENCES profile_members(member_id),
    type text NOT NULL CHECK (type IN ('JOINED', 'PROJECT', 'EVENT', 'AWARD', 'MANAGEMENT', 'OTHER')),
    title text NOT NULL,
    description text,
    happened_at timestamptz NOT NULL,
    public_visible boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_profile_member_milestones_member_id ON profile_member_milestones(member_id);

CREATE TABLE profile_member_work_snapshots (
    id uuid PRIMARY KEY,
    work_id text NOT NULL UNIQUE,
    member_id text NOT NULL REFERENCES profile_members(member_id),
    type text NOT NULL CHECK (type IN ('BUILD', 'REDSTONE', 'FARM', 'ARTICLE', 'IMAGE', 'VIDEO', 'OTHER')),
    title text NOT NULL,
    summary text,
    cover_url text,
    source_module text,
    source_id text,
    public_visible boolean NOT NULL DEFAULT true,
    sort_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_profile_member_work_snapshots_member_id ON profile_member_work_snapshots(member_id);
