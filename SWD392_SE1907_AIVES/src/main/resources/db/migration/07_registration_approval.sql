ALTER TABLE users ADD COLUMN IF NOT EXISTS registration_status VARCHAR(20);
UPDATE users SET registration_status = 'APPROVED' WHERE registration_status IS NULL;
ALTER TABLE users ALTER COLUMN registration_status SET DEFAULT 'APPROVED';
