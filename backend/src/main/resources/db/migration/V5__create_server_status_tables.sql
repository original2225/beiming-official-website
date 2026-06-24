CREATE TABLE server_status_sources (
    id uuid PRIMARY KEY,
    source_id text NOT NULL UNIQUE,
    instance_id text NOT NULL UNIQUE,
    display_name text NOT NULL,
    instance_name text NOT NULL,
    instance_kind text NOT NULL CHECK (instance_kind IN ('SURVIVAL', 'CREATIVE', 'TEST', 'LOBBY', 'OTHER')),
    source_type text NOT NULL CHECK (source_type IN ('MINECRAFT_PING', 'HTTP_HEALTH', 'MANUAL', 'STUB')),
    config_status text NOT NULL CHECK (config_status IN ('ENABLED', 'DISABLED', 'ARCHIVED')),
    public_visible boolean NOT NULL DEFAULT true,
    primary_source boolean NOT NULL DEFAULT false,
    target text NOT NULL,
    timeout_ms integer NOT NULL CHECK (timeout_ms >= 500 AND timeout_ms <= 10000),
    sort_order integer NOT NULL DEFAULT 100,
    started_at timestamptz,
    admin_note text,
    created_by text NOT NULL,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_server_status_sources_config_status ON server_status_sources(config_status);
CREATE INDEX idx_server_status_sources_public_visible ON server_status_sources(public_visible);

CREATE TABLE server_status_lines (
    id uuid PRIMARY KEY,
    line_id text NOT NULL UNIQUE,
    name text NOT NULL,
    entry_address text NOT NULL,
    check_target text NOT NULL,
    description text,
    config_status text NOT NULL CHECK (config_status IN ('ENABLED', 'DISABLED', 'ARCHIVED')),
    current_status text NOT NULL CHECK (current_status IN ('AVAILABLE', 'DEGRADED', 'UNAVAILABLE', 'UNKNOWN')),
    public_visible boolean NOT NULL DEFAULT true,
    primary_line boolean NOT NULL DEFAULT false,
    sort_order integer NOT NULL DEFAULT 100,
    latency_ms integer,
    packet_loss_percent numeric(8, 3),
    admin_note text,
    created_by text NOT NULL,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_server_status_lines_config_status ON server_status_lines(config_status);
CREATE INDEX idx_server_status_lines_public_visible ON server_status_lines(public_visible);

CREATE TABLE server_status_snapshots (
    id uuid PRIMARY KEY,
    snapshot_id text NOT NULL UNIQUE,
    source_id text NOT NULL,
    instance_id text NOT NULL,
    line_id text,
    source text NOT NULL CHECK (source IN ('SCHEDULED', 'MANUAL_REFRESH', 'SEED', 'DEGRADED_FALLBACK')),
    status text NOT NULL CHECK (status IN ('ONLINE', 'DEGRADED', 'OFFLINE', 'UNKNOWN')),
    line_status text NOT NULL CHECK (line_status IN ('AVAILABLE', 'DEGRADED', 'UNAVAILABLE', 'UNKNOWN')),
    version text,
    motd text,
    online_players integer NOT NULL CHECK (online_players >= 0),
    max_players integer NOT NULL CHECK (max_players >= 0),
    latency_ms integer,
    line_latency_ms integer,
    checked_at timestamptz NOT NULL,
    degraded boolean NOT NULL DEFAULT false,
    raw_summary jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_server_status_snapshots_instance_checked ON server_status_snapshots(instance_id, checked_at DESC);
CREATE INDEX idx_server_status_snapshots_source_id ON server_status_snapshots(source_id);

CREATE TABLE server_status_outages (
    id uuid PRIMARY KEY,
    outage_id text NOT NULL UNIQUE,
    title text NOT NULL,
    public_message text NOT NULL,
    status text NOT NULL CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'ARCHIVED')),
    severity text NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    instance_id text,
    line_id text,
    started_at timestamptz NOT NULL,
    resolved_at timestamptz,
    acknowledged_by text,
    resolved_by text,
    archived_by text,
    acknowledged_at timestamptz,
    archived_at timestamptz,
    internal_reason text,
    admin_note text,
    public_visible boolean NOT NULL DEFAULT true,
    created_by text NOT NULL,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    deleted_at timestamptz
);

CREATE INDEX idx_server_status_outages_status ON server_status_outages(status);
CREATE INDEX idx_server_status_outages_instance_line ON server_status_outages(instance_id, line_id);

CREATE TABLE server_status_refresh_records (
    id uuid PRIMARY KEY,
    refresh_id text NOT NULL UNIQUE,
    source_id text NOT NULL,
    snapshot_id text,
    status text NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED', 'CONFLICT')),
    idempotency_key text,
    reason text,
    requested_by text NOT NULL,
    started_at timestamptz NOT NULL,
    completed_at timestamptz,
    failure_reason text,
    result_summary jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_server_status_refresh_records_source_id ON server_status_refresh_records(source_id);
CREATE INDEX idx_server_status_refresh_records_idempotency_key ON server_status_refresh_records(idempotency_key);
