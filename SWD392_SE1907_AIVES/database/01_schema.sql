-- AIVES | PostgreSQL 16+ | Modules 1-3 only
-- Run in an empty database. All timestamps are timestamptz (UTC in storage).
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ========= SHARED FOUNDATIONS =========
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'LECTURER', 'STUDENT')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_users_email_lowercase CHECK (email = LOWER(email))
);

CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE course_lecturers (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    lecturer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (course_id, lecturer_id)
);

CREATE TABLE course_enrollments (
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    enrollment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (enrollment_status IN ('ACTIVE', 'DROPPED')),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (course_id, student_id)
);

-- ========= MODULE 1: QUESTION BANK / RUBRICS / RAG =========
CREATE TABLE course_topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    description TEXT,
    UNIQUE (course_id, slug),
    UNIQUE (id, course_id)
);

-- Binary documents / audio stay in object storage; store only their object keys.
CREATE TABLE course_materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    original_filename VARCHAR(255) NOT NULL,
    object_key TEXT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_sha256 CHAR(64),
    extraction_status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED'
        CHECK (extraction_status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    extracted_text_object_key TEXT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id, course_id)
);

-- external_vector_id can point to Qdrant/Pinecone; optional pgvector add-on is separate.
CREATE TABLE material_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL REFERENCES course_materials(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    content TEXT NOT NULL CHECK (LENGTH(TRIM(content)) > 0),
    token_count INTEGER CHECK (token_count >= 0),
    page_number INTEGER CHECK (page_number >= 1),
    external_vector_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (material_id, chunk_index)
);

CREATE TABLE question_generation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    instruction TEXT,
    desired_count INTEGER NOT NULL CHECK (desired_count BETWEEN 1 AND 100),
    target_bloom_level VARCHAR(20) CHECK (
        target_bloom_level IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE')
    ),
    model_name VARCHAR(120),
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    generated_count INTEGER NOT NULL DEFAULT 0 CHECK (generated_count >= 0),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    UNIQUE (id, course_id)
);

CREATE TABLE question_generation_materials (
    job_id UUID NOT NULL,
    material_id UUID NOT NULL,
    course_id UUID NOT NULL,
    PRIMARY KEY (job_id, material_id),
    FOREIGN KEY (job_id, course_id)
        REFERENCES question_generation_jobs(id, course_id) ON DELETE CASCADE,
    FOREIGN KEY (material_id, course_id)
        REFERENCES course_materials(id, course_id) ON DELETE CASCADE
);

CREATE TABLE rubrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(180) NOT NULL,
    description TEXT,
    max_score NUMERIC(6,2) NOT NULL CHECK (max_score > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id, course_id)
);

CREATE TABLE rubric_criteria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rubric_id UUID NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    criterion_name VARCHAR(180) NOT NULL,
    description TEXT,
    max_points NUMERIC(6,2) NOT NULL CHECK (max_points > 0),
    sort_order INTEGER NOT NULL CHECK (sort_order >= 1),
    UNIQUE (rubric_id, sort_order)
);

CREATE TABLE question_import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    imported_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    original_filename VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    total_rows INTEGER NOT NULL DEFAULT 0 CHECK (total_rows >= 0),
    imported_rows INTEGER NOT NULL DEFAULT 0 CHECK (imported_rows >= 0),
    error_details JSONB NOT NULL DEFAULT '[]'::jsonb,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_import_counts CHECK (imported_rows <= total_rows),
    UNIQUE (id, course_id)
);

CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    topic_id UUID,
    rubric_id UUID,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    content TEXT NOT NULL CHECK (LENGTH(TRIM(content)) > 0),
    reference_answer TEXT,
    bloom_level VARCHAR(20) NOT NULL CHECK (
        bloom_level IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE')
    ),
    difficulty VARCHAR(15) NOT NULL DEFAULT 'MEDIUM'
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    source_type VARCHAR(15) NOT NULL
        CHECK (source_type IN ('MANUAL', 'IMPORTED', 'AI')),
    generation_job_id UUID,
    import_batch_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED')),
    approved_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id, course_id),
    FOREIGN KEY (topic_id, course_id)
        REFERENCES course_topics(id, course_id) ON DELETE RESTRICT,
    FOREIGN KEY (rubric_id, course_id)
        REFERENCES rubrics(id, course_id) ON DELETE RESTRICT,
    FOREIGN KEY (generation_job_id, course_id)
        REFERENCES question_generation_jobs(id, course_id) ON DELETE RESTRICT,
    FOREIGN KEY (import_batch_id, course_id)
        REFERENCES question_import_batches(id, course_id) ON DELETE RESTRICT,
    CONSTRAINT ck_question_ai_has_job CHECK (source_type <> 'AI' OR generation_job_id IS NOT NULL),
    CONSTRAINT ck_question_import_has_batch CHECK (source_type <> 'IMPORTED' OR import_batch_id IS NOT NULL),
    CONSTRAINT ck_approved_question_has_rubric CHECK (status <> 'APPROVED' OR rubric_id IS NOT NULL),
    CONSTRAINT ck_approved_question_has_approver CHECK (
        status <> 'APPROVED' OR (approved_by IS NOT NULL AND approved_at IS NOT NULL)
    )
);

CREATE TABLE question_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decision VARCHAR(20) NOT NULL
        CHECK (decision IN ('APPROVE', 'REJECT', 'REQUEST_CHANGES')),
    comment TEXT,
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Provenance: which passages supported an AI-generated question.
CREATE TABLE question_source_chunks (
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    chunk_id UUID NOT NULL REFERENCES material_chunks(id) ON DELETE RESTRICT,
    relevance_score NUMERIC(5,4) CHECK (relevance_score BETWEEN 0 AND 1),
    PRIMARY KEY (question_id, chunk_id)
);

-- ========= MODULE 2: EXAMS AND SCHEDULING =========
CREATE TABLE exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    instructions TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SCHEDULED', 'ACTIVE', 'CLOSED', 'CANCELLED')),
    selection_mode VARCHAR(15) NOT NULL DEFAULT 'RANDOM'
        CHECK (selection_mode IN ('RANDOM', 'ADAPTIVE')),
    main_question_count INTEGER NOT NULL CHECK (main_question_count BETWEEN 1 AND 100),
    max_followups_per_question INTEGER NOT NULL DEFAULT 2
        CHECK (max_followups_per_question BETWEEN 0 AND 10),
    max_followups_per_student INTEGER CHECK (max_followups_per_student BETWEEN 0 AND 100),
    answer_time_limit_seconds INTEGER NOT NULL DEFAULT 120
        CHECK (answer_time_limit_seconds BETWEEN 10 AND 3600),
    slot_duration_minutes INTEGER NOT NULL DEFAULT 20
        CHECK (slot_duration_minutes BETWEEN 1 AND 240),
    exam_language VARCHAR(5) NOT NULL DEFAULT 'vi' CHECK (exam_language IN ('vi', 'en')),
    exam_timezone VARCHAR(80) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    starts_at TIMESTAMPTZ,
    ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (id, course_id),
    CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at)
);

-- Composite foreign keys guarantee question and exam belong to the same course.
CREATE TABLE exam_question_pool (
    exam_id UUID NOT NULL,
    question_id UUID NOT NULL,
    course_id UUID NOT NULL,
    selection_weight INTEGER NOT NULL DEFAULT 1 CHECK (selection_weight BETWEEN 1 AND 100),
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (exam_id, question_id),
    FOREIGN KEY (exam_id, course_id)
        REFERENCES exams(id, course_id) ON DELETE CASCADE,
    FOREIGN KEY (question_id, course_id)
        REFERENCES questions(id, course_id) ON DELETE RESTRICT
);

CREATE TABLE exam_candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID NOT NULL,
    course_id UUID NOT NULL,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED'
        CHECK (status IN ('REGISTERED', 'READY', 'IN_PROGRESS', 'COMPLETED', 'ABSENT', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (exam_id, student_id),
    UNIQUE (id, exam_id),
    FOREIGN KEY (exam_id, course_id)
        REFERENCES exams(id, course_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id, student_id)
        REFERENCES course_enrollments(course_id, student_id) ON DELETE RESTRICT,
    CHECK (scheduled_start < scheduled_end)
);

CREATE TABLE exam_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL,
    exam_id UUID NOT NULL,
    attempt_no INTEGER NOT NULL DEFAULT 1 CHECK (attempt_no >= 1),
    status VARCHAR(20) NOT NULL DEFAULT 'READY'
        CHECK (status IN ('READY', 'IN_PROGRESS', 'COMPLETED', 'ABORTED')),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (candidate_id, attempt_no),
    UNIQUE (id, exam_id),
    FOREIGN KEY (candidate_id, exam_id)
        REFERENCES exam_candidates(id, exam_id) ON DELETE CASCADE,
    CHECK (started_at IS NULL OR ended_at IS NULL OR started_at <= ended_at)
);

-- The exact question text is frozen so later edits to the bank do not alter a past exam.
CREATE TABLE attempt_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_id UUID NOT NULL,
    exam_id UUID NOT NULL,
    question_id UUID NOT NULL,
    sequence_no INTEGER NOT NULL CHECK (sequence_no >= 1),
    question_text_snapshot TEXT NOT NULL,
    reference_answer_snapshot TEXT,
    rubric_snapshot TEXT,
    selection_mode VARCHAR(15) NOT NULL CHECK (selection_mode IN ('RANDOM', 'ADAPTIVE', 'MANUAL')),
    selection_reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ASKING', 'ANSWERING', 'FINISHED', 'TIMED_OUT')),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (attempt_id, sequence_no),
    UNIQUE (attempt_id, question_id),
    FOREIGN KEY (attempt_id, exam_id)
        REFERENCES exam_attempts(id, exam_id) ON DELETE CASCADE,
    FOREIGN KEY (exam_id, question_id)
        REFERENCES exam_question_pool(exam_id, question_id) ON DELETE RESTRICT,
    CHECK (started_at IS NULL OR completed_at IS NULL OR started_at <= completed_at)
);

-- ========= MODULE 3: AI VIVA INTERVIEW (TTS / STT / FOLLOW-UPS) =========
-- One final transcript row per spoken turn; live STT partials update that row.
CREATE TABLE interview_turns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_question_id UUID NOT NULL REFERENCES attempt_questions(id) ON DELETE CASCADE,
    turn_no INTEGER NOT NULL CHECK (turn_no >= 1),
    speaker VARCHAR(10) NOT NULL CHECK (speaker IN ('AI', 'STUDENT')),
    kind VARCHAR(25) NOT NULL CHECK (kind IN (
        'MAIN_QUESTION', 'MAIN_ANSWER', 'FOLLOWUP_QUESTION', 'FOLLOWUP_ANSWER', 'SYSTEM_NOTE'
    )),
    followup_number INTEGER NOT NULL DEFAULT 0 CHECK (followup_number BETWEEN 0 AND 10),
    reply_to_turn_id UUID REFERENCES interview_turns(id) ON DELETE SET NULL,
    text_content TEXT,
    audio_object_key TEXT,
    speech_status VARCHAR(20) NOT NULL DEFAULT 'NONE'
        CHECK (speech_status IN ('NONE', 'QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    stt_confidence NUMERIC(5,4) CHECK (stt_confidence BETWEEN 0 AND 1),
    ai_model_name VARCHAR(120),
    followup_reason TEXT,
    response_deadline_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (attempt_question_id, turn_no),
    CHECK (
        (speaker = 'AI' AND kind IN ('MAIN_QUESTION', 'FOLLOWUP_QUESTION', 'SYSTEM_NOTE')) OR
        (speaker = 'STUDENT' AND kind IN ('MAIN_ANSWER', 'FOLLOWUP_ANSWER'))
    ),
    CHECK (
        (kind IN ('FOLLOWUP_QUESTION', 'FOLLOWUP_ANSWER') AND followup_number > 0) OR
        (kind NOT IN ('FOLLOWUP_QUESTION', 'FOLLOWUP_ANSWER') AND followup_number = 0)
    ),
    CHECK (started_at IS NULL OR ended_at IS NULL OR started_at <= ended_at)
);

-- Request telemetry for AI/STT/TTS workers; text/audio is kept in interview_turns / object storage.
CREATE TABLE ai_interview_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attempt_question_id UUID NOT NULL REFERENCES attempt_questions(id) ON DELETE CASCADE,
    input_turn_id UUID REFERENCES interview_turns(id) ON DELETE SET NULL,
    output_turn_id UUID REFERENCES interview_turns(id) ON DELETE SET NULL,
    request_type VARCHAR(25) NOT NULL
        CHECK (request_type IN ('FOLLOWUP_GENERATION', 'STT', 'TTS')),
    provider VARCHAR(80),
    model_name VARCHAR(120),
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
        CHECK (status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    latency_ms INTEGER CHECK (latency_ms >= 0),
    input_tokens INTEGER CHECK (input_tokens >= 0),
    output_tokens INTEGER CHECK (output_tokens >= 0),
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_message TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ
);

-- ========= AUTOMATIC updated_at =========
CREATE FUNCTION touch_updated_at() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_rubrics_updated BEFORE UPDATE ON rubrics FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_questions_updated BEFORE UPDATE ON questions FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
CREATE TRIGGER trg_exams_updated BEFORE UPDATE ON exams FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ========= USEFUL INDEXES =========
CREATE INDEX idx_enrollments_student ON course_enrollments(student_id);
CREATE INDEX idx_courses_lecturer ON course_lecturers(lecturer_id);
CREATE INDEX idx_topics_course ON course_topics(course_id);
CREATE INDEX idx_materials_course ON course_materials(course_id, extraction_status);
CREATE INDEX idx_chunks_material ON material_chunks(material_id, chunk_index);
CREATE INDEX idx_chunks_fts_simple ON material_chunks USING GIN (to_tsvector('simple', content));
CREATE INDEX idx_generation_jobs_course ON question_generation_jobs(course_id, status);
CREATE INDEX idx_questions_catalog ON questions(course_id, status, bloom_level, difficulty);
CREATE INDEX idx_questions_topic ON questions(topic_id);
CREATE INDEX idx_reviews_question ON question_reviews(question_id, reviewed_at DESC);
CREATE INDEX idx_exam_course_status ON exams(course_id, status);
CREATE INDEX idx_exam_candidates_student ON exam_candidates(student_id, scheduled_start);
CREATE INDEX idx_exam_candidates_slots ON exam_candidates(exam_id, scheduled_start);
CREATE INDEX idx_attempts_exam ON exam_attempts(exam_id, status);
CREATE INDEX idx_attempt_questions_history ON attempt_questions(exam_id, question_id, created_at DESC);
CREATE INDEX idx_turns_timeline ON interview_turns(attempt_question_id, turn_no);
CREATE INDEX idx_ai_requests_attempt ON ai_interview_requests(attempt_question_id, requested_at DESC);
CREATE UNIQUE INDEX ux_one_main_question_turn ON interview_turns(attempt_question_id)
    WHERE kind = 'MAIN_QUESTION';
CREATE UNIQUE INDEX ux_one_followup_number ON interview_turns(attempt_question_id, followup_number)
    WHERE kind = 'FOLLOWUP_QUESTION';
CREATE UNIQUE INDEX ux_one_final_answer_per_prompt ON interview_turns(attempt_question_id, kind, followup_number)
    WHERE kind IN ('MAIN_ANSWER', 'FOLLOWUP_ANSWER');

COMMENT ON TABLE questions IS 'Module 1: teacher-created, imported, and AI-generated questions; review workflow.';
COMMENT ON TABLE exam_question_pool IS 'Module 2: approved candidate questions that this exam can assign.';
COMMENT ON TABLE interview_turns IS 'Module 3: main question, main answer, adaptive follow-up and answer in sequence.';
COMMENT ON COLUMN material_chunks.external_vector_id IS 'Optional vector-search ID if embeddings live outside PostgreSQL.';
COMMENT ON COLUMN interview_turns.audio_object_key IS 'S3/MinIO storage key, not raw audio or a permanent public URL.';
COMMIT;
