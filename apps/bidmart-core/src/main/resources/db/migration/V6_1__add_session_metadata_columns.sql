-- Add session metadata columns for device, location, and login timestamps

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'sessions'
    ) THEN
        ALTER TABLE sessions
            ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE,
            ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE,
            ADD COLUMN IF NOT EXISTS device_info VARCHAR(512),
            ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64),
            ADD COLUMN IF NOT EXISTS location_label VARCHAR(255);

        UPDATE sessions
        SET created_at = COALESCE(created_at, NOW()),
            last_login_at = COALESCE(last_login_at, NOW())
        WHERE created_at IS NULL OR last_login_at IS NULL;

        ALTER TABLE sessions
            ALTER COLUMN created_at SET NOT NULL,
            ALTER COLUMN last_login_at SET NOT NULL;

        CREATE INDEX IF NOT EXISTS idx_sessions_user_active_last_login
            ON sessions (user_id, is_active, last_login_at);
    END IF;
END $$;

