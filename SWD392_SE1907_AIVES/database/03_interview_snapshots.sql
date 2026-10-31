-- Run once if schema auto-update is disabled. Does not modify existing answers.
ALTER TABLE attempt_questions ADD COLUMN IF NOT EXISTS reference_answer_snapshot TEXT;
ALTER TABLE attempt_questions ADD COLUMN IF NOT EXISTS rubric_snapshot TEXT;
