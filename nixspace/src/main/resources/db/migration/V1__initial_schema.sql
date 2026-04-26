-- ============================================================
-- NixSpace Database Schema — V1 Initial Migration
-- Engine: MySQL 8.x  |  Charset: utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    email            VARCHAR(255)    NOT NULL,
    password_hash    VARCHAR(255)    NOT NULL,
    display_name     VARCHAR(100)    NOT NULL,
    avatar_url       VARCHAR(512),
    status_text      VARCHAR(100),
    timezone         VARCHAR(100)    NOT NULL DEFAULT 'UTC',
    is_active        BOOLEAN         NOT NULL DEFAULT TRUE,
    is_email_verified BOOLEAN        NOT NULL DEFAULT FALSE,
    last_seen_at     DATETIME(6),
    created_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_is_active (is_active),
    INDEX idx_users_display_name (display_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_tokens (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(255) NOT NULL,
    device_info  VARCHAR(255),
    ip_address   VARCHAR(45),
    expires_at   DATETIME(6)  NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    INDEX idx_refresh_tokens_user_id (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- WORKSPACES
-- ============================================================
CREATE TABLE workspaces (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    description   TEXT,
    icon_url      VARCHAR(512),
    owner_user_id BIGINT       NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspaces_slug (slug),
    INDEX idx_workspaces_owner (owner_user_id),
    CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- WORKSPACE MEMBERS
-- ============================================================
CREATE TABLE workspace_members (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    role         VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- OWNER | ADMIN | MEMBER | GUEST
    joined_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_workspace_members (workspace_id, user_id),
    INDEX idx_workspace_members_user (user_id),
    CONSTRAINT fk_wm_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_wm_user     FOREIGN KEY (user_id)      REFERENCES users (id)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- CHANNELS
-- ============================================================
CREATE TABLE channels (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    type         VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC',  -- PUBLIC | PRIVATE | DM | GROUP_DM
    topic        VARCHAR(250),
    description  TEXT,
    created_by   BIGINT       NOT NULL,
    is_archived  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_channels_workspace_name (workspace_id, name),
    INDEX idx_channels_workspace (workspace_id),
    INDEX idx_channels_type (type),
    CONSTRAINT fk_channels_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_channels_creator  FOREIGN KEY (created_by)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- CHANNEL MEMBERS
-- ============================================================
CREATE TABLE channel_members (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    channel_id            BIGINT      NOT NULL,
    user_id               BIGINT      NOT NULL,
    role                  VARCHAR(20) NOT NULL DEFAULT 'MEMBER',  -- ADMIN | MEMBER
    last_read_message_id  BIGINT,
    notification_pref     VARCHAR(20) NOT NULL DEFAULT 'ALL',  -- ALL | MENTIONS | NOTHING
    joined_at             DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_members (channel_id, user_id),
    INDEX idx_channel_members_user (user_id),
    CONSTRAINT fk_cm_channel FOREIGN KEY (channel_id) REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- MESSAGES
-- ============================================================
CREATE TABLE messages (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    channel_id        BIGINT       NOT NULL,
    sender_user_id    BIGINT       NOT NULL,
    parent_message_id BIGINT,                              -- NULL = top-level; set = thread reply
    message_type      VARCHAR(20)  NOT NULL DEFAULT 'USER', -- USER | SYSTEM | BOT
    content           TEXT         NOT NULL,
    content_type      VARCHAR(20)  NOT NULL DEFAULT 'PLAIN', -- PLAIN | MARKDOWN
    is_edited         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    reply_count       INT          NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_messages_channel_created (channel_id, created_at),
    INDEX idx_messages_parent (parent_message_id),
    INDEX idx_messages_sender (sender_user_id),
    INDEX idx_messages_channel_id (channel_id),
    FULLTEXT INDEX ft_messages_content (content),
    CONSTRAINT fk_messages_channel FOREIGN KEY (channel_id)     REFERENCES channels (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender  FOREIGN KEY (sender_user_id) REFERENCES users (id),
    CONSTRAINT fk_messages_parent  FOREIGN KEY (parent_message_id) REFERENCES messages (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- MESSAGE REACTIONS
-- ============================================================
CREATE TABLE message_reactions (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    message_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    reaction   VARCHAR(50) NOT NULL,  -- emoji code e.g. :thumbsup:
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_reactions (message_id, user_id, reaction),
    INDEX idx_reactions_message (message_id),
    CONSTRAINT fk_reactions_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- FILES
-- ============================================================
CREATE TABLE files (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    workspace_id  BIGINT       NOT NULL,
    uploaded_by   BIGINT       NOT NULL,
    storage_key   VARCHAR(512) NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    thumbnail_key VARCHAR(512),
    virus_scan_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | CLEAN | INFECTED
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_files_storage_key (storage_key),
    INDEX idx_files_workspace (workspace_id),
    INDEX idx_files_uploaded_by (uploaded_by),
    CONSTRAINT fk_files_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_files_uploader  FOREIGN KEY (uploaded_by)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- MESSAGE FILES (join table)
-- ============================================================
CREATE TABLE message_files (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    file_id    BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_files (message_id, file_id),
    CONSTRAINT fk_mf_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_mf_file    FOREIGN KEY (file_id)    REFERENCES files (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(50)  NOT NULL,  -- MENTION | REPLY | CHANNEL_INVITE | DM | REACTION
    entity_type VARCHAR(50)  NOT NULL,  -- MESSAGE | CHANNEL | WORKSPACE
    entity_id   BIGINT       NOT NULL,
    actor_id    BIGINT,                 -- who triggered the notification
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_notifications_user_unread (user_id, is_read, created_at),
    INDEX idx_notifications_user (user_id),
    CONSTRAINT fk_notifications_user  FOREIGN KEY (user_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- EMAIL VERIFICATION TOKENS
-- ============================================================
CREATE TABLE email_verification_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at DATETIME(6)  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_evtoken (token),
    CONSTRAINT fk_evtoken_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
