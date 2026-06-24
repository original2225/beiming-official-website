CREATE TABLE admin_settings (
    id uuid PRIMARY KEY,
    setting_key text NOT NULL UNIQUE,
    scope text NOT NULL CHECK (scope IN ('GLOBAL', 'MODULE', 'DASHBOARD', 'NAVIGATION', 'AUDIT')),
    value_type text NOT NULL CHECK (value_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'JSON', 'SECRET')),
    setting_value text NOT NULL,
    sensitive boolean NOT NULL DEFAULT false,
    high_impact boolean NOT NULL DEFAULT false,
    description text,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_admin_settings_scope ON admin_settings(scope);

CREATE TABLE admin_layouts (
    id uuid PRIMARY KEY,
    layout_key text NOT NULL UNIQUE,
    dashboard_cards jsonb NOT NULL DEFAULT '[]'::jsonb,
    navigation_module_order jsonb NOT NULL DEFAULT '[]'::jsonb,
    hidden_modules jsonb NOT NULL DEFAULT '[]'::jsonb,
    quick_actions jsonb NOT NULL DEFAULT '[]'::jsonb,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE TABLE admin_module_indexes (
    id uuid PRIMARY KEY,
    module_key text NOT NULL UNIQUE,
    status text NOT NULL CHECK (status IN ('AVAILABLE', 'DEGRADED', 'UNAVAILABLE', 'NOT_IMPLEMENTED', 'DISABLED')),
    implemented boolean NOT NULL,
    enabled boolean NOT NULL,
    sort_order integer NOT NULL DEFAULT 100,
    badge_count integer NOT NULL DEFAULT 0,
    target_api_base text,
    frontend_route text NOT NULL,
    health_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    indexed_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_admin_module_indexes_status ON admin_module_indexes(status);

CREATE TABLE admin_todo_indexes (
    id uuid PRIMARY KEY,
    todo_id text NOT NULL UNIQUE,
    source_module text NOT NULL,
    source_type text NOT NULL,
    source_id text NOT NULL,
    type text NOT NULL CHECK (type IN ('REVIEW', 'CONFIG', 'FAILURE', 'HEALTH', 'SECURITY', 'FOLLOW_UP')),
    severity text NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status text NOT NULL CHECK (status IN ('OPEN', 'READ_ONLY', 'SOURCE_UNAVAILABLE', 'STALE')),
    title text NOT NULL,
    summary text,
    target_route text,
    target_api text,
    read_only boolean NOT NULL DEFAULT true,
    indexed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_admin_todo_indexes_source_status ON admin_todo_indexes(source_module, status);

CREATE TABLE admin_metric_snapshots (
    id uuid PRIMARY KEY,
    metric_key text NOT NULL UNIQUE,
    label text NOT NULL,
    source_module text NOT NULL,
    metric_value numeric(18, 3),
    unit text NOT NULL,
    trend jsonb,
    target_route text,
    degraded boolean NOT NULL DEFAULT false,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_admin_metric_snapshots_source ON admin_metric_snapshots(source_module);

CREATE TABLE admin_audit_indexes (
    id uuid PRIMARY KEY,
    audit_index_id text NOT NULL UNIQUE,
    source_module text NOT NULL,
    source_audit_id text NOT NULL,
    request_id text NOT NULL,
    actor_user_id text,
    actor_display_name text,
    actor_role text NOT NULL,
    target_type text NOT NULL,
    target_id text NOT NULL,
    action text NOT NULL,
    risk_level text NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    result text NOT NULL CHECK (result IN ('SUCCESS', 'FAILED')),
    reason_summary text,
    failure_reason text,
    target_route text,
    indexed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_admin_audit_indexes_request_id ON admin_audit_indexes(request_id);
CREATE INDEX idx_admin_audit_indexes_action ON admin_audit_indexes(action);
CREATE INDEX idx_admin_audit_indexes_target ON admin_audit_indexes(target_type, target_id);

CREATE TABLE admin_setting_change_records (
    id uuid PRIMARY KEY,
    change_id text NOT NULL UNIQUE,
    idempotency_key text NOT NULL,
    actor_user_id text NOT NULL,
    request_id text NOT NULL,
    reason text NOT NULL,
    changed_settings jsonb NOT NULL DEFAULT '[]'::jsonb,
    layout_patch jsonb NOT NULL DEFAULT '{}'::jsonb,
    result text NOT NULL CHECK (result IN ('SUCCESS', 'FAILED')),
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_admin_setting_change_records_idempotency_key ON admin_setting_change_records(idempotency_key);
CREATE INDEX idx_admin_setting_change_records_request_id ON admin_setting_change_records(request_id);
