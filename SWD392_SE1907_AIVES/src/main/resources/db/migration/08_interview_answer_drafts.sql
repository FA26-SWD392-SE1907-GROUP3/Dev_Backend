CREATE TABLE IF NOT EXISTS interview_answer_drafts (
    prompt_id UUID PRIMARY KEY,
    attempt_id UUID NOT NULL,
    revision BIGINT NOT NULL,
    text_content TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
-- Hibernate update may have created the table before this compatibility migration runs.
ALTER TABLE interview_answer_drafts DROP CONSTRAINT IF EXISTS fk_interview_draft_prompt;
ALTER TABLE interview_answer_drafts ADD CONSTRAINT fk_interview_draft_prompt FOREIGN KEY (prompt_id) REFERENCES interview_turns(id) ON DELETE CASCADE;
ALTER TABLE interview_answer_drafts DROP CONSTRAINT IF EXISTS fk_interview_draft_attempt;
ALTER TABLE interview_answer_drafts ADD CONSTRAINT fk_interview_draft_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id) ON DELETE CASCADE;
ALTER TABLE interview_answer_drafts DROP CONSTRAINT IF EXISTS ck_interview_draft_revision;
ALTER TABLE interview_answer_drafts ADD CONSTRAINT ck_interview_draft_revision CHECK (revision >= 0);
ALTER TABLE interview_answer_drafts DROP CONSTRAINT IF EXISTS ck_interview_draft_length;
ALTER TABLE interview_answer_drafts ADD CONSTRAINT ck_interview_draft_length CHECK (char_length(text_content) <= 12000);
CREATE INDEX IF NOT EXISTS idx_interview_answer_drafts_attempt ON interview_answer_drafts(attempt_id);
