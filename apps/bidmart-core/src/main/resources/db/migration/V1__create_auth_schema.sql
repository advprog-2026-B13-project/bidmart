-- Initial auth schema

CREATE SEQUENCE IF NOT EXISTS role_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS permission_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS roles (
    id INTEGER NOT NULL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS permissions (
    id INTEGER NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id UUID NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    photo_url VARCHAR(1024),
    shipping_address VARCHAR(200),
    default_2fa_method VARCHAR(16) NOT NULL,
    role_id INTEGER,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT chk_users_default_2fa_method CHECK (default_2fa_method IN ('TOTP', 'EMAIL', 'DISABLED')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS email_otps (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_email_otps_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS totp_credentials (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    secret_key VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_totp_credentials_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS recovery_codes (
    id UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    code_hash VARCHAR(255) NOT NULL,
    is_used BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_recovery_codes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_user_active ON sessions (user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users (role_id);

INSERT INTO roles (id, name)
VALUES (nextval('role_seq'), 'USER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (id, name)
VALUES
    (nextval('permission_seq'), 'auction:create'),
    (nextval('permission_seq'), 'auction:delete'),
    (nextval('permission_seq'), 'account:deactivate'),
    (nextval('permission_seq'), 'user:manage'),
    (nextval('permission_seq'), 'admin:all')
ON CONFLICT (name) DO NOTHING;

