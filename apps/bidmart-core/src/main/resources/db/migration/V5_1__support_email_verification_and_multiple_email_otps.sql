-- Support registration email verification and reusable email OTP records

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'users'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;
        ALTER TABLE users
            ADD CONSTRAINT chk_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED'));
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'email_otps'
    ) THEN
        ALTER TABLE email_otps DROP CONSTRAINT IF EXISTS email_otps_user_id_key;
        CREATE INDEX IF NOT EXISTS idx_email_otps_user_id ON email_otps(user_id);
    END IF;
END $$;
