-- DEMO DATA ONLY. Password of every demo user: Demo@123456
-- Run after 01_schema.sql in a disposable/local development database.
BEGIN;
INSERT INTO users (id, full_name, email, password_hash, role) VALUES
('10000000-0000-4000-8000-000000000001', 'System Admin', 'admin@aives.local', crypt('Demo@123456', gen_salt('bf')), 'ADMIN'),
('10000000-0000-4000-8000-000000000002', 'Nguyen Van Giang', 'lecturer@aives.local', crypt('Demo@123456', gen_salt('bf')), 'LECTURER'),
('10000000-0000-4000-8000-000000000003', 'Tran Minh An', 'student1@aives.local', crypt('Demo@123456', gen_salt('bf')), 'STUDENT'),
('10000000-0000-4000-8000-000000000004', 'Le Ha My', 'student2@aives.local', crypt('Demo@123456', gen_salt('bf')), 'STUDENT');

INSERT INTO courses (id, code, name, description) VALUES
('20000000-0000-4000-8000-000000000001', 'PRJ301', 'Lập trình Web', 'Môn học demo cho hệ thống AIVES');
INSERT INTO course_lecturers (course_id, lecturer_id) VALUES
('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002');
INSERT INTO course_enrollments (course_id, student_id) VALUES
('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003'),
('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000004');

INSERT INTO course_topics (id, course_id, name, slug, description) VALUES
('30000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'HTTP & REST API', 'http-rest', 'HTTP methods, request/response, REST'),
('30000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001', 'Database & SQL', 'database-sql', 'Relational database and SQL');

INSERT INTO course_materials
(id, course_id, uploaded_by, original_filename, object_key, mime_type, extraction_status)
VALUES ('40000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'web-fundamentals.pdf',
'courses/PRJ301/materials/web-fundamentals.pdf', 'application/pdf', 'READY');
INSERT INTO material_chunks (id, material_id, chunk_index, content, token_count, page_number) VALUES
('41000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', 0,
'HTTP là giao thức ứng dụng. GET dùng để lấy tài nguyên; POST thường tạo tài nguyên; PUT dùng thay thế tài nguyên; DELETE dùng xóa tài nguyên.', 48, 1),
('41000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000001', 1,
'Khóa chính giúp định danh bản ghi. Khóa ngoại giúp duy trì tính toàn vẹn tham chiếu. JOIN kết hợp dữ liệu giữa các bảng.', 49, 2);

INSERT INTO question_generation_jobs
(id, course_id, requested_by, instruction, desired_count, target_bloom_level, model_name, status, generated_count, completed_at)
VALUES ('42000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Sinh câu hỏi ứng dụng từ bài giảng.', 2, 'APPLY',
'demo-model', 'COMPLETED', 2, NOW());
INSERT INTO question_generation_materials (job_id, material_id, course_id) VALUES
('42000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001');

INSERT INTO rubrics (id, course_id, created_by, name, description, max_score, status) VALUES
('50000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Rubric trả lời vấn đáp cơ bản', 'Thang đánh giá cấu trúc câu trả lời', 10, 'PUBLISHED');
INSERT INTO rubric_criteria (rubric_id, criterion_name, description, max_points, sort_order) VALUES
('50000000-0000-4000-8000-000000000001', 'Kiến thức chính xác', 'Thuật ngữ và thông tin đúng', 4, 1),
('50000000-0000-4000-8000-000000000001', 'Giải thích và lập luận', 'Có phân tích, giải thích', 3, 2),
('50000000-0000-4000-8000-000000000001', 'Ví dụ minh họa', 'Liên hệ thực tế', 3, 3);

INSERT INTO question_import_batches
(id, course_id, imported_by, original_filename, status, total_rows, imported_rows)
VALUES ('51000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'questions.csv', 'COMPLETED', 1, 1);

-- Four approved questions in the exam pool; one pending question demonstrates AI review.
INSERT INTO questions
(id, course_id, topic_id, rubric_id, created_by, content, reference_answer,
 bloom_level, difficulty, source_type, generation_job_id, import_batch_id, status, approved_by, approved_at)
VALUES
('60000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001',
'30000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Phân biệt HTTP GET và POST, kèm ví dụ.',
'GET lấy dữ liệu, POST thường tạo tài nguyên; giải thích semantics và ví dụ.', 'UNDERSTAND', 'EASY', 'MANUAL', NULL, NULL,
'APPROVED', '10000000-0000-4000-8000-000000000002', NOW()),
('60000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001',
'30000000-0000-4000-8000-000000000002', '50000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Khóa chính và khóa ngoại khác nhau như thế nào?',
'Khóa chính định danh hàng, khóa ngoại tham chiếu bảng khác.', 'UNDERSTAND', 'EASY', 'IMPORTED', NULL,
'51000000-0000-4000-8000-000000000001', 'APPROVED', '10000000-0000-4000-8000-000000000002', NOW()),
('60000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000001',
'30000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Thiết kế REST API cho chức năng tạo sinh viên.',
'POST /students, validate payload, trả mã 201 và Location.', 'APPLY', 'MEDIUM', 'AI',
'42000000-0000-4000-8000-000000000001', NULL, 'APPROVED',
'10000000-0000-4000-8000-000000000002', NOW()),
('60000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000001',
'30000000-0000-4000-8000-000000000002', '50000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Nêu ví dụ dùng JOIN để lấy tên sinh viên và tên môn học.',
'JOIN enrollments với users và courses theo khóa ngoại.', 'APPLY', 'MEDIUM', 'MANUAL', NULL, NULL,
'APPROVED', '10000000-0000-4000-8000-000000000002', NOW()),
('60000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000001',
'30000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Phân tích lý do không nên dùng GET để thay đổi dữ liệu.',
'GET là safe method; side-effects vi phạm semantics, có rủi ro cache/prefetch.', 'ANALYZE', 'HARD', 'MANUAL', NULL, NULL,
'APPROVED', '10000000-0000-4000-8000-000000000002', NOW()),
('60000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000001',
'30000000-0000-4000-8000-000000000002', '50000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Tại sao nên dùng transaction khi cập nhật nhiều bảng?',
'Atomicity và rollback khi lỗi.', 'APPLY', 'MEDIUM', 'AI',
'42000000-0000-4000-8000-000000000001', NULL, 'PENDING_REVIEW', NULL, NULL);

INSERT INTO question_reviews (question_id, reviewer_id, decision, comment) VALUES
('60000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000002', 'APPROVE', 'Phù hợp chuẩn đầu ra.'),
('60000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000002', 'REQUEST_CHANGES', 'Cần thêm bối cảnh thực tế.');
INSERT INTO question_source_chunks (question_id, chunk_id, relevance_score) VALUES
('60000000-0000-4000-8000-000000000003', '41000000-0000-4000-8000-000000000001', 0.9200),
('60000000-0000-4000-8000-000000000006', '41000000-0000-4000-8000-000000000002', 0.8500);

-- Future exam: ready for scheduling workflow.
INSERT INTO exams
(id, course_id, created_by, title, instructions, status, selection_mode, main_question_count,
 max_followups_per_question, answer_time_limit_seconds, slot_duration_minutes, starts_at, ends_at)
VALUES
('70000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Thi vấn đáp PRJ301 - kỳ sắp tới',
'Mỗi sinh viên trả lời 2 câu chính, mỗi câu tối đa 2 câu hỏi xoáy.', 'SCHEDULED', 'RANDOM', 2, 2, 90, 20,
NOW() + INTERVAL '7 days', NOW() + INTERVAL '7 days 3 hours'),
('70000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001',
'10000000-0000-4000-8000-000000000002', 'Buổi thi demo đã hoàn tất',
'Mẫu dữ liệu transcript AI.', 'CLOSED', 'ADAPTIVE', 1, 2, 120, 20,
NOW() - INTERVAL '1 day', NOW() - INTERVAL '22 hours');

INSERT INTO exam_question_pool (exam_id, question_id, course_id, selection_weight)
SELECT e.id, q.id, e.course_id, 1
FROM exams e CROSS JOIN questions q
WHERE e.course_id = q.course_id
AND e.id IN ('70000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000002')
AND q.status = 'APPROVED';

INSERT INTO exam_candidates
(id, exam_id, course_id, student_id, scheduled_start, scheduled_end, status)
VALUES
('80000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001',
'20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003',
NOW() + INTERVAL '7 days 10 minutes', NOW() + INTERVAL '7 days 30 minutes', 'REGISTERED'),
('80000000-0000-4000-8000-000000000002', '70000000-0000-4000-8000-000000000001',
'20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000004',
NOW() + INTERVAL '7 days 40 minutes', NOW() + INTERVAL '7 days 60 minutes', 'REGISTERED'),
('80000000-0000-4000-8000-000000000003', '70000000-0000-4000-8000-000000000002',
'20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003',
NOW() - INTERVAL '23 hours 50 minutes', NOW() - INTERVAL '23 hours 30 minutes', 'COMPLETED');

INSERT INTO exam_attempts
(id, candidate_id, exam_id, attempt_no, status, started_at, ended_at, last_activity_at)
VALUES
('81000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000003',
'70000000-0000-4000-8000-000000000002', 1, 'COMPLETED',
NOW() - INTERVAL '23 hours 49 minutes', NOW() - INTERVAL '23 hours 43 minutes',
NOW() - INTERVAL '23 hours 43 minutes');

INSERT INTO attempt_questions
(id, attempt_id, exam_id, question_id, sequence_no, question_text_snapshot, selection_mode,
 selection_reason, status, started_at, completed_at)
VALUES
('82000000-0000-4000-8000-000000000001', '81000000-0000-4000-8000-000000000001',
'70000000-0000-4000-8000-000000000002', '60000000-0000-4000-8000-000000000001',
1, 'Phân biệt HTTP GET và POST, kèm ví dụ.', 'ADAPTIVE', 'Mở đầu ở mức UNDERSTAND', 'FINISHED',
NOW() - INTERVAL '23 hours 49 minutes', NOW() - INTERVAL '23 hours 43 minutes');

INSERT INTO interview_turns
(id, attempt_question_id, turn_no, speaker, kind, followup_number, reply_to_turn_id,
 text_content, speech_status, stt_confidence, ai_model_name, followup_reason)
VALUES
('90000000-0000-4000-8000-000000000001', '82000000-0000-4000-8000-000000000001',
1, 'AI', 'MAIN_QUESTION', 0, NULL, 'Phân biệt HTTP GET và POST, kèm ví dụ.', 'SUCCEEDED', NULL, 'demo-tts', NULL),
('90000000-0000-4000-8000-000000000002', '82000000-0000-4000-8000-000000000001',
2, 'STUDENT', 'MAIN_ANSWER', 0, '90000000-0000-4000-8000-000000000001',
'GET để đọc dữ liệu, POST để gửi dữ liệu lên server.', 'SUCCEEDED', 0.9500, NULL, NULL),
('90000000-0000-4000-8000-000000000003', '82000000-0000-4000-8000-000000000001',
3, 'AI', 'FOLLOWUP_QUESTION', 1, '90000000-0000-4000-8000-000000000002',
'Nếu muốn tạo mới một sinh viên, em chọn GET hay POST? Vì sao?',
'SUCCEEDED', NULL, 'demo-llm', 'Câu trả lời thiếu ví dụ tạo tài nguyên.'),
('90000000-0000-4000-8000-000000000004', '82000000-0000-4000-8000-000000000001',
4, 'STUDENT', 'FOLLOWUP_ANSWER', 1, '90000000-0000-4000-8000-000000000003',
'Dùng POST /students vì đây là thao tác tạo tài nguyên mới.', 'SUCCEEDED', 0.9700, NULL, NULL);

INSERT INTO ai_interview_requests
(attempt_question_id, input_turn_id, output_turn_id, request_type, provider,
 model_name, status, latency_ms, input_tokens, output_tokens, parameters, finished_at)
VALUES
('82000000-0000-4000-8000-000000000001', '90000000-0000-4000-8000-000000000002',
'90000000-0000-4000-8000-000000000003', 'FOLLOWUP_GENERATION', 'demo-provider',
'demo-llm', 'SUCCEEDED', 940, 180, 35, '{"followup_index":1,"max_followups":2}'::jsonb, NOW());
COMMIT;
