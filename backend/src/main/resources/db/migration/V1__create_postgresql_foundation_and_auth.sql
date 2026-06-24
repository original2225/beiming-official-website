CREATE TABLE app_request_logs (
    id uuid PRIMARY KEY,
    request_id text NOT NULL UNIQUE,
    method text NOT NULL,
    path text NOT NULL,
    actor_user_id text,
    source_ip text,
    response_code integer,
    result text NOT NULL CHECK (result IN ('SUCCESS', 'FAILED')),
    failure_reason text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE app_audit_logs (
    id uuid PRIMARY KEY,
    request_id text NOT NULL,
    actor_user_id text,
    actor_role text NOT NULL,
    actor_permissions jsonb NOT NULL DEFAULT '[]'::jsonb,
    source_ip text,
    target_type text NOT NULL,
    target_id text NOT NULL,
    action text NOT NULL,
    risk_level text NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    reason text,
    params_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    before_state jsonb,
    after_state jsonb,
    result text NOT NULL CHECK (result IN ('SUCCESS', 'FAILED')),
    failure_reason text,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_audit_logs_request_id ON app_audit_logs(request_id);
CREATE INDEX idx_app_audit_logs_action ON app_audit_logs(action);

CREATE TABLE app_idempotency_records (
    id uuid PRIMARY KEY,
    actor_user_id text NOT NULL,
    scope text NOT NULL,
    idempotency_key text NOT NULL,
    request_fingerprint text NOT NULL,
    response_code integer NOT NULL,
    response_body jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    UNIQUE (actor_user_id, scope, idempotency_key)
);

CREATE TABLE auth_users (
    id uuid PRIMARY KEY,
    user_id text NOT NULL UNIQUE,
    username text NOT NULL UNIQUE,
    username_normalized text NOT NULL UNIQUE,
    display_name text NOT NULL,
    display_name_normalized text NOT NULL UNIQUE,
    password_hash text NOT NULL,
    roles jsonb NOT NULL DEFAULT '[]'::jsonb,
    permissions jsonb NOT NULL DEFAULT '[]'::jsonb,
    status text NOT NULL CHECK (status IN ('PENDING_PROFILE', 'ACTIVE', 'DISABLED', 'BANNED', 'DELETED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    last_login_at timestamptz
);

CREATE INDEX idx_auth_users_status ON auth_users(status);

CREATE TABLE auth_sessions (
    id uuid PRIMARY KEY,
    session_id text NOT NULL UNIQUE,
    token_hash text NOT NULL UNIQUE,
    user_id text NOT NULL REFERENCES auth_users(user_id),
    created_at timestamptz NOT NULL,
    last_seen_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked boolean NOT NULL DEFAULT false,
    revoked_at timestamptz
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions(user_id);

CREATE TABLE auth_invitations (
    id uuid PRIMARY KEY,
    invitation_id text NOT NULL UNIQUE,
    code_prefix text NOT NULL,
    code_hash text NOT NULL,
    type text NOT NULL CHECK (type IN ('PLAYER', 'ADMIN')),
    bound_roles jsonb NOT NULL DEFAULT '[]'::jsonb,
    bound_permissions jsonb NOT NULL DEFAULT '[]'::jsonb,
    max_uses integer NOT NULL CHECK (max_uses > 0),
    used_count integer NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    expires_at timestamptz,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL,
    disabled_at timestamptz
);

CREATE INDEX idx_auth_invitations_code_prefix ON auth_invitations(code_prefix);

CREATE TABLE auth_invitation_usage_records (
    id uuid PRIMARY KEY,
    usage_id text NOT NULL UNIQUE,
    invitation_id text NOT NULL REFERENCES auth_invitations(invitation_id),
    used_by_user_id text NOT NULL REFERENCES auth_users(user_id),
    used_by_username text NOT NULL,
    source_ip text,
    request_id text NOT NULL,
    used_at timestamptz NOT NULL
);

CREATE TABLE auth_minecraft_bindings (
    id uuid PRIMARY KEY,
    user_id text NOT NULL UNIQUE REFERENCES auth_users(user_id),
    minecraft_id text NOT NULL UNIQUE,
    minecraft_uuid text NOT NULL UNIQUE,
    verified_at timestamptz NOT NULL,
    source text NOT NULL
);

CREATE TABLE auth_password_reset_tokens (
    id uuid PRIMARY KEY,
    reset_token_hash text NOT NULL UNIQUE,
    user_id text NOT NULL REFERENCES auth_users(user_id),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used boolean NOT NULL DEFAULT false
);
