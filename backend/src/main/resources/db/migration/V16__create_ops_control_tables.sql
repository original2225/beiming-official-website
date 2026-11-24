CREATE TABLE ops_nodes (
    id uuid PRIMARY KEY,
    node_id text NOT NULL UNIQUE,
    display_name text NOT NULL,
    endpoint_summary text NOT NULL,
    status text NOT NULL CHECK (status IN ('PENDING_REGISTRATION', 'ONLINE', 'OFFLINE', 'DISABLED', 'REVOKED')),
    version text,
    capabilities jsonb NOT NULL DEFAULT '[]'::jsonb,
    labels jsonb NOT NULL DEFAULT '{}'::jsonb,
    last_heartbeat_at timestamptz,
    last_seen_request_id text,
    token_digest text NOT NULL,
    created_by text NOT NULL,
    updated_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ops_nodes_status ON ops_nodes(status);
CREATE INDEX idx_ops_nodes_last_heartbeat_at ON ops_nodes(last_heartbeat_at);

CREATE TABLE ops_runtime_snapshots (
    id uuid PRIMARY KEY,
    snapshot_type text NOT NULL CHECK (snapshot_type IN ('CONTAINER', 'VM', 'MINECRAFT_INSTANCE', 'FILE')),
    node_id text NOT NULL,
    target_id text NOT NULL,
    status text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (snapshot_type, node_id, target_id)
);

CREATE INDEX idx_ops_runtime_snapshots_node_id ON ops_runtime_snapshots(node_id);
CREATE INDEX idx_ops_runtime_snapshots_type ON ops_runtime_snapshots(snapshot_type);

CREATE TABLE ops_metrics (
    id uuid PRIMARY KEY,
    snapshot_id text NOT NULL UNIQUE,
    node_id text NOT NULL,
    cpu_usage_percent double precision NOT NULL,
    memory_usage_percent double precision NOT NULL,
    disk_usage_percent double precision NOT NULL,
    collected_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_ops_metrics_node_id ON ops_metrics(node_id, collected_at DESC);

CREATE TABLE ops_tasks (
    id uuid PRIMARY KEY,
    task_id text NOT NULL UNIQUE,
    task_type text NOT NULL,
    status text NOT NULL CHECK (status IN ('QUEUED', 'DISPATCHED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'TIMEOUT', 'CANCELED', 'PENDING_APPROVAL')),
    risk_level text NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    node_id text,
    target_type text NOT NULL,
    target_id text NOT NULL,
    reason text NOT NULL,
    params_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    approval_id text,
    idempotency_key text,
    node_request_id text,
    result_summary jsonb,
    failure_reason text,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL
);

CREATE INDEX idx_ops_tasks_status ON ops_tasks(status);
CREATE INDEX idx_ops_tasks_node_id ON ops_tasks(node_id);
CREATE INDEX idx_ops_tasks_created_by ON ops_tasks(created_by);

CREATE TABLE ops_approvals (
    id uuid PRIMARY KEY,
    approval_id text NOT NULL UNIQUE,
    task_id text NOT NULL,
    status text NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED')),
    risk_level text NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    requested_by text NOT NULL,
    approved_by text,
    review_comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    reviewed_at timestamptz,
    expires_at timestamptz NOT NULL
);

CREATE INDEX idx_ops_approvals_status ON ops_approvals(status);
CREATE INDEX idx_ops_approvals_task_id ON ops_approvals(task_id);
