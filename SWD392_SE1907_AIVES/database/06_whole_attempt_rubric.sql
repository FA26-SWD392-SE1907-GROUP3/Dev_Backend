-- The application runs this same idempotent migration from src/main/resources/db/migration.
-- No existing questions, answers, rubrics or historical snapshots are deleted.
ALTER TABLE questions DROP CONSTRAINT IF EXISTS ck_approved_question_has_rubric;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS bloom_level VARCHAR(20);
ALTER TABLE questions ALTER COLUMN bloom_level DROP NOT NULL;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS rubric_id UUID REFERENCES rubrics(id);
ALTER TABLE question_generation_jobs ADD COLUMN IF NOT EXISTS target_difficulty VARCHAR(15);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS rubric_snapshot TEXT;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS grading_status VARCHAR(20);
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS grading_error TEXT;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS grading_result TEXT;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS grading_requested_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS graded_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX IF NOT EXISTS idx_attempts_grading_queue ON exam_attempts(status, grading_status, ended_at);
