CREATE TABLE notification_templates (
    id uuid PRIMARY KEY,
    template_id text NOT NULL UNIQUE,
    code text NOT NULL UNIQUE,
    name text NOT NULL,
    title_template text NOT NULL,
    body_template text NOT NULL,
    variable_definitions jsonb NOT NULL DEFAULT '[]'::jsonb,
    type text NOT NULL CHECK (type IN ('SYSTEM', 'AUDIT', 'WHITELIST', 'EXAM', 'CONTENT', 'RESOURCE', 'ATTENDANCE', 'COMMUNITY', 'ACTIVITY', 'OPS')),
    channels jsonb NOT NULL DEFAULT '["IN_APP"]'::jsonb,
    status text NOT NULL CHECK (status IN ('ENABLED', 'DISABLED')),
    version integer NOT NULL CHECK (version > 0),
    created_by text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_by text NOT NULL,
    updated_at timestamptz NOT NULL,
    disabled_at timestamptz
);

CREATE INDEX idx_notification_templates_status ON notification_templates(status);
CREATE INDEX idx_notification_templates_type ON notification_templates(type);

CREATE TABLE notification_messages (
    id uuid PRIMARY KEY,
    notification_id text NOT NULL UNIQUE,
    title text NOT NULL,
    body text NOT NULL,
    type text NOT NULL CHECK (type IN ('SYSTEM', 'AUDIT', 'WHITELIST', 'EXAM', 'CONTENT', 'RESOURCE', 'ATTENDANCE', 'COMMUNITY', 'ACTIVITY', 'OPS')),
    channels jsonb NOT NULL DEFAULT '["IN_APP"]'::jsonb,
    source_module text,
    source_id text,
    risk_level text NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    action_url text,
    template_id text,
    template_code text,
    template_version integer,
    variables jsonb,
    created_by text NOT NULL,
    created_at timestamptz NOT NULL,
    expires_at timestamptz
);

CREATE INDEX idx_notification_messages_created_at ON notification_messages(created_at);
CREATE INDEX idx_notification_messages_type ON notification_messages(type);
CREATE INDEX idx_notification_messages_source ON notification_messages(source_module, source_id);

CREATE TABLE notification_recipients (
    id uuid PRIMARY KEY,
    notification_id text NOT NULL REFERENCES notification_messages(notification_id),
    recipient_user_id text NOT NULL,
    recipient_display_name_snapshot text NOT NULL,
    status text NOT NULL CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED')),
    delivery_status text NOT NULL CHECK (delivery_status IN ('DELIVERED', 'FAILED')),
    failure_reason text,
    read_at timestamptz,
    archived_at timestamptz,
    delivered_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (notification_id, recipient_user_id)
);

CREATE INDEX idx_notification_recipients_user_status ON notification_recipients(recipient_user_id, status);
CREATE INDEX idx_notification_recipients_notification_id ON notification_recipients(notification_id);
