-- Idempotent migration. Legacy values become the total cap for one student's whole attempt.
ALTER TABLE exams ADD COLUMN IF NOT EXISTS max_followups_per_student INTEGER;
UPDATE exams SET max_followups_per_student = max_followups_per_question
WHERE max_followups_per_student IS NULL;
-- The application validates new values in the 0..100 range. The old column stays for compatibility.
