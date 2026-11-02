CREATE TABLE IF NOT EXISTS exam_retake_grants (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL,
    previous_attempt_id UUID NOT NULL,
    next_attempt_no INTEGER NOT NULL,
    granted_by UUID NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end TIMESTAMPTZ NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);
ALTER TABLE exam_retake_grants DROP CONSTRAINT IF EXISTS fk_retake_candidate;
ALTER TABLE exam_retake_grants ADD CONSTRAINT fk_retake_candidate FOREIGN KEY (candidate_id) REFERENCES exam_candidates(id);
ALTER TABLE exam_retake_grants DROP CONSTRAINT IF EXISTS fk_retake_previous;
ALTER TABLE exam_retake_grants ADD CONSTRAINT fk_retake_previous FOREIGN KEY (previous_attempt_id) REFERENCES exam_attempts(id);
ALTER TABLE exam_retake_grants DROP CONSTRAINT IF EXISTS fk_retake_lecturer;
ALTER TABLE exam_retake_grants ADD CONSTRAINT fk_retake_lecturer FOREIGN KEY (granted_by) REFERENCES users(id);
ALTER TABLE exam_retake_grants DROP CONSTRAINT IF EXISTS ck_retake_schedule;
ALTER TABLE exam_retake_grants ADD CONSTRAINT ck_retake_schedule CHECK (scheduled_start < scheduled_end AND next_attempt_no > 1);
CREATE UNIQUE INDEX IF NOT EXISTS uq_retake_pending_candidate ON exam_retake_grants(candidate_id) WHERE consumed_at IS NULL AND revoked_at IS NULL;
