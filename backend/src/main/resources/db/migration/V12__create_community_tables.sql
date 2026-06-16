CREATE TABLE community_boards (
    id uuid PRIMARY KEY,
    board_id text NOT NULL UNIQUE,
    slug text NOT NULL UNIQUE,
    name text NOT NULL,
    description text NOT NULL,
    visibility text NOT NULL CHECK (visibility IN ('PUBLIC', 'MEMBER_ONLY', 'STAFF_ONLY')),
    status text NOT NULL CHECK (status IN ('DRAFT', 'ACTIVE', 'LOCKED', 'ARCHIVED')),
    allowed_post_types jsonb NOT NULL DEFAULT '[]'::jsonb,
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    sort_order integer NOT NULL DEFAULT 0,
    last_post_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    archived_at timestamptz
);

CREATE INDEX idx_community_boards_status_visibility ON community_boards(status, visibility);
CREATE INDEX idx_community_boards_sort_order ON community_boards(sort_order);

CREATE TABLE community_posts (
    id uuid PRIMARY KEY,
    post_id text NOT NULL UNIQUE,
    board_id text NOT NULL REFERENCES community_boards(board_id),
    type text NOT NULL CHECK (type IN ('DISCUSSION', 'QUESTION', 'GUIDE', 'SHOWCASE', 'SUGGESTION', 'ANNOUNCEMENT_DISCUSSION', 'RESOURCE_DISCUSSION')),
    title text NOT NULL,
    summary text,
    body text NOT NULL,
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    status text NOT NULL CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'NEEDS_CHANGES', 'REJECTED', 'LOCKED', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    author_user_id text NOT NULL,
    author_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    linked_content_snapshot jsonb,
    linked_resource_snapshot jsonb,
    poll_id text,
    like_count integer NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    favorite_count integer NOT NULL DEFAULT 0 CHECK (favorite_count >= 0),
    view_count integer NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    accepted_comment_id text,
    last_comment_at timestamptz,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    reviewer_user_id text,
    review_comment text,
    notification_status text,
    notification_failure jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    offline_at timestamptz,
    archived_at timestamptz,
    deleted_at timestamptz
);

CREATE INDEX idx_community_posts_board_id ON community_posts(board_id);
CREATE INDEX idx_community_posts_status ON community_posts(status);
CREATE INDEX idx_community_posts_author_user_id ON community_posts(author_user_id);
CREATE INDEX idx_community_posts_created_at ON community_posts(created_at);
CREATE INDEX idx_community_posts_last_comment_at ON community_posts(last_comment_at);

CREATE TABLE community_comments (
    id uuid PRIMARY KEY,
    comment_id text NOT NULL UNIQUE,
    post_id text NOT NULL REFERENCES community_posts(post_id),
    parent_comment_id text,
    body text NOT NULL,
    status text NOT NULL CHECK (status IN ('PENDING_REVIEW', 'APPROVED', 'NEEDS_CHANGES', 'REJECTED', 'OFFLINE', 'ARCHIVED', 'DELETED')),
    author_user_id text NOT NULL,
    author_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    like_count integer NOT NULL DEFAULT 0 CHECK (like_count >= 0),
    is_accepted_answer boolean NOT NULL DEFAULT false,
    submitted_at timestamptz,
    reviewed_at timestamptz,
    review_comment text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    deleted_at timestamptz
);

CREATE INDEX idx_community_comments_post_id ON community_comments(post_id);
CREATE INDEX idx_community_comments_status ON community_comments(status);
CREATE INDEX idx_community_comments_author_user_id ON community_comments(author_user_id);
CREATE INDEX idx_community_comments_created_at ON community_comments(created_at);

CREATE TABLE community_reactions (
    id uuid PRIMARY KEY,
    reaction_id text NOT NULL UNIQUE,
    target_type text NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    target_id text NOT NULL,
    actor_user_id text NOT NULL,
    reaction_type text NOT NULL CHECK (reaction_type IN ('LIKE')),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (target_type, target_id, actor_user_id, reaction_type)
);

CREATE INDEX idx_community_reactions_target ON community_reactions(target_type, target_id);
CREATE INDEX idx_community_reactions_actor ON community_reactions(actor_user_id);

CREATE TABLE community_favorites (
    id uuid PRIMARY KEY,
    favorite_id text NOT NULL UNIQUE,
    post_id text NOT NULL REFERENCES community_posts(post_id),
    actor_user_id text NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (post_id, actor_user_id)
);

CREATE INDEX idx_community_favorites_actor ON community_favorites(actor_user_id);

CREATE TABLE community_polls (
    id uuid PRIMARY KEY,
    poll_id text NOT NULL UNIQUE,
    post_id text NOT NULL REFERENCES community_posts(post_id),
    title text NOT NULL,
    description text,
    status text NOT NULL CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'ARCHIVED')),
    options_payload jsonb NOT NULL DEFAULT '[]'::jsonb,
    multiple_choice boolean NOT NULL DEFAULT false,
    min_choices integer NOT NULL CHECK (min_choices > 0),
    max_choices integer NOT NULL CHECK (max_choices > 0),
    eligible_visibility text NOT NULL CHECK (eligible_visibility IN ('PUBLIC', 'MEMBER_ONLY', 'STAFF_ONLY')),
    anonymous_result boolean NOT NULL DEFAULT true,
    vote_count integer NOT NULL DEFAULT 0 CHECK (vote_count >= 0),
    opens_at timestamptz,
    closes_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_community_polls_post_id ON community_polls(post_id);
CREATE INDEX idx_community_polls_status ON community_polls(status);

CREATE TABLE community_poll_votes (
    id uuid PRIMARY KEY,
    vote_id text NOT NULL UNIQUE,
    poll_id text NOT NULL REFERENCES community_polls(poll_id),
    actor_user_id text NOT NULL,
    option_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL,
    UNIQUE (poll_id, actor_user_id)
);

CREATE INDEX idx_community_poll_votes_actor ON community_poll_votes(actor_user_id);

CREATE TABLE community_reports (
    id uuid PRIMARY KEY,
    report_id text NOT NULL UNIQUE,
    target_type text NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    target_id text NOT NULL,
    reason_type text NOT NULL CHECK (reason_type IN ('SPAM', 'HARASSMENT', 'INAPPROPRIATE', 'COPYRIGHT', 'IMPERSONATION', 'GAME_VIOLATION', 'OTHER')),
    description text NOT NULL,
    evidence_links jsonb NOT NULL DEFAULT '[]'::jsonb,
    status text NOT NULL CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED', 'ESCALATED', 'ARCHIVED')),
    reporter_user_id text NOT NULL,
    reporter_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    assignee_user_id text,
    resolution text,
    linked_penalty_id text,
    notification_status text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    resolved_at timestamptz
);

CREATE INDEX idx_community_reports_target ON community_reports(target_type, target_id);
CREATE INDEX idx_community_reports_status ON community_reports(status);
CREATE INDEX idx_community_reports_reporter ON community_reports(reporter_user_id);

CREATE TABLE community_tickets (
    id uuid PRIMARY KEY,
    ticket_id text NOT NULL UNIQUE,
    type text NOT NULL CHECK (type IN ('BAN_APPEAL', 'WHITELIST_ISSUE', 'ACCOUNT_ISSUE', 'RESOURCE_ISSUE', 'BUG_REPORT', 'CONTENT_DISPUTE', 'OTHER')),
    title text NOT NULL,
    status text NOT NULL CHECK (status IN ('OPEN', 'WAITING_STAFF', 'WAITING_USER', 'RESOLVED', 'CLOSED', 'ARCHIVED')),
    priority text NOT NULL,
    creator_user_id text NOT NULL,
    creator_snapshot jsonb NOT NULL DEFAULT '{}'::jsonb,
    assignee_user_id text,
    related_object jsonb,
    last_reply_at timestamptz,
    resolved_at timestamptz,
    closed_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_community_tickets_status ON community_tickets(status);
CREATE INDEX idx_community_tickets_creator ON community_tickets(creator_user_id);
CREATE INDEX idx_community_tickets_assignee ON community_tickets(assignee_user_id);

CREATE TABLE community_ticket_messages (
    id uuid PRIMARY KEY,
    message_id text NOT NULL UNIQUE,
    ticket_id text NOT NULL REFERENCES community_tickets(ticket_id),
    message_type text NOT NULL CHECK (message_type IN ('USER_REPLY', 'STAFF_REPLY', 'INTERNAL_NOTE', 'SYSTEM_EVENT')),
    body text NOT NULL,
    author_user_id text,
    author_snapshot jsonb,
    attachments jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL
);

CREATE INDEX idx_community_ticket_messages_ticket_id ON community_ticket_messages(ticket_id);

CREATE TABLE community_penalties (
    id uuid PRIMARY KEY,
    penalty_id text NOT NULL UNIQUE,
    target_user_id text NOT NULL,
    target_member_id text NOT NULL,
    type text NOT NULL CHECK (type IN ('WARNING', 'MUTE', 'BAN', 'WHITELIST_REVIEW_REQUIRED', 'POST_RESTRICTED', 'SUBMISSION_RESTRICTED')),
    status text NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED', 'ARCHIVED')),
    reason text,
    public_reason text NOT NULL,
    evidence_report_id text,
    related_post_id text,
    related_comment_id text,
    starts_at timestamptz,
    expires_at timestamptz,
    created_by text NOT NULL,
    revoked_by text,
    revoked_at timestamptz,
    revoke_reason text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_community_penalties_target_user ON community_penalties(target_user_id);
CREATE INDEX idx_community_penalties_status ON community_penalties(status);
CREATE INDEX idx_community_penalties_type ON community_penalties(type);
