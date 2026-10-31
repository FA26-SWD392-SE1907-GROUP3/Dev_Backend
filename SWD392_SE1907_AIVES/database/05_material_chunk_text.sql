-- Preserve full extracted paragraphs instead of a 255-character field. Safe to run again.
ALTER TABLE material_chunks ALTER COLUMN content TYPE TEXT;
-- The generated question and answer must retain full text from these paragraphs as well.
ALTER TABLE questions ALTER COLUMN content TYPE TEXT;
ALTER TABLE questions ALTER COLUMN reference_answer TYPE TEXT;
ALTER TABLE attempt_questions ALTER COLUMN question_text_snapshot TYPE TEXT;
