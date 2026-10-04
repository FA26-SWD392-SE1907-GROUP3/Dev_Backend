-- Local test data only. Run AFTER 02-functional-schema.sql in SSMS.
-- All demo accounts use Aives@123 (BCrypt). Existing records are not overwritten.
-- No document/audio rows are fabricated: upload real files through the application.
USE AIVES;
GO
SET XACT_ABORT ON;
IF COL_LENGTH('dbo.ExamSessions','AnswerTimeLimitSeconds') IS NULL
    THROW 50010, 'Run 02-functional-schema.sql before this seed.', 1;

BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @now datetime2(6) = SYSDATETIME();
 DECLARE @hash nvarchar(255) = N'$2a$10$ZhNOgK0lWb6fU.3dG7xHtuDgb1MZTF0MQPX/XvKUdvUkASy2Dp4he';

 INSERT dbo.Roles(RoleName,Description)
 SELECT v.RoleName,v.Description FROM (VALUES
 ('ADMIN',N'Quản trị viên'),('LECTURER',N'Giảng viên'),('STUDENT',N'Sinh viên')
 ) v(RoleName,Description)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.Roles r WHERE r.RoleName=v.RoleName);

 INSERT dbo.Users(RoleID,Username,PasswordHash,FullName,Email,CreatedAt,user_status)
 SELECT r.RoleID,v.Username,@hash,v.FullName,v.Email,@now,'ACTIVE'
 FROM (VALUES
 ('ADMIN','demo_admin',N'Quản trị viên demo','demo_admin@aives.local'),
 ('LECTURER','demo_lecturer',N'Giảng viên Nguyễn Minh','demo_lecturer@aives.local'),
 ('STUDENT','demo_student01',N'Sinh viên Nguyễn Quốc','demo_student01@aives.local'),
 ('STUDENT','demo_student02',N'Sinh viên Trần An','demo_student02@aives.local'),
 ('STUDENT','demo_student03',N'Sinh viên Lê Bình','demo_student03@aives.local')
 ) v(RoleName,Username,FullName,Email)
 JOIN dbo.Roles r ON r.RoleName=v.RoleName
 WHERE NOT EXISTS(SELECT 1 FROM dbo.Users u WHERE u.Username=v.Username);

 DECLARE @lecturer int = (SELECT UserID FROM dbo.Users WHERE Username='demo_lecturer');
 IF NOT EXISTS(SELECT 1 FROM dbo.Users u JOIN dbo.Roles r ON r.RoleID=u.RoleID
               WHERE u.UserID=@lecturer AND r.RoleName='LECTURER' AND u.user_status='ACTIVE')
    THROW 50011, 'demo_lecturer exists with an incompatible role or status.', 1;

 INSERT dbo.Subjects(SubjectCode,SubjectName,Description)
 SELECT v.Code,v.Name,N'Dữ liệu mẫu AIVES để kiểm thử'
 FROM (VALUES
 (N'DEMO_SWD392',N'Software Architecture and Design'),
 (N'DEMO_SWP391',N'Software Development Project'),
 (N'DEMO_DBI202',N'Database Systems')
 ) v(Code,Name)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.Subjects s WHERE s.SubjectCode=v.Code);
 DECLARE @subject int = (SELECT SubjectID FROM dbo.Subjects WHERE SubjectCode=N'DEMO_SWD392');

 DECLARE @questions TABLE(Content nvarchar(1000),Bloom varchar(50),Status varchar(50),Topic nvarchar(150));
 INSERT @questions VALUES
 (N'[DEMO] SOLID gồm những nguyên tắc nào?', 'REMEMBER','APPROVED',N'SOLID'),
 (N'[DEMO] Giải thích sự khác nhau giữa kiến trúc monolithic và microservices.', 'UNDERSTAND','APPROVED',N'Kiến trúc phần mềm'),
 (N'[DEMO] Áp dụng Dependency Injection vào chức năng quản lý câu hỏi như thế nào?', 'APPLY','APPROVED',N'Dependency Injection'),
 (N'[DEMO] Phân tích ưu và nhược điểm của kiến trúc phân lớp trong AIVES.', 'ANALYZE','APPROVED',N'Layered Architecture'),
 (N'[DEMO] Repository Pattern giải quyết vấn đề gì?', 'UNDERSTAND','PENDING_APPROVAL',N'Design Patterns'),
 (N'[DEMO] Có nên lưu mật khẩu người dùng dưới dạng văn bản thuần?', 'ANALYZE','REJECTED',N'Bảo mật');

 INSERT dbo.Questions(SubjectID,CreatedBy,Topic,QuestionContent,BloomLevel,Source,Status,CreatedAt,ApprovedBy,ApprovedAt)
 SELECT @subject,@lecturer,q.Topic,q.Content,q.Bloom,'MANUAL',q.Status,@now,
        CASE WHEN q.Status='APPROVED' THEN @lecturer END,
        CASE WHEN q.Status='APPROVED' THEN @now END
 FROM @questions q
 WHERE NOT EXISTS(SELECT 1 FROM dbo.Questions existing
                  WHERE existing.SubjectID=@subject AND existing.CreatedBy=@lecturer AND existing.QuestionContent=q.Content);

 INSERT dbo.Rubrics(QuestionID,CriteriaName,MaxScore,Guideline,CreatedAt)
 SELECT q.QuestionID,v.Name,v.Score,v.Guide,@now
 FROM dbo.Questions q JOIN @questions seed ON seed.Content=q.QuestionContent
 CROSS JOIN (VALUES
 (N'Kiến thức chính xác',CAST(5 AS decimal(5,2)),N'Nêu đúng khái niệm và các ý chính liên quan đến câu hỏi.'),
 (N'Giải thích và lập luận',CAST(3 AS decimal(5,2)),N'Giải thích rõ, lập luận hợp lý và có liên kết giữa các ý.'),
 (N'Ví dụ thực tế',CAST(2 AS decimal(5,2)),N'Đưa ra ví dụ phù hợp với hệ thống AIVES hoặc dự án phần mềm.')
 ) v(Name,Score,Guide)
 WHERE q.SubjectID=@subject AND q.CreatedBy=@lecturer
 AND NOT EXISTS(SELECT 1 FROM dbo.Rubrics r WHERE r.QuestionID=q.QuestionID AND r.CriteriaName=v.Name);

 -- Use current server time. Rerunning preserves original slots and exam progress.
 INSERT dbo.ExamSessions(SubjectID,CreatedBy,ExamName,StartTime,EndTime,MaxMainQuestions,MaxFollowUpQuestions,Status,AnswerTimeLimitSeconds)
 SELECT @subject,@lecturer,v.Name,DATEADD(minute,-5,@now),DATEADD(day,1,@now),2,v.FollowUp,v.Status,120
 FROM (VALUES
 (N'[DEMO] Thi thử SWD392 - không cần AI',0,'PUBLISHED'),
 (N'[DEMO] Kỳ thi nháp - cấu hình AI',1,'DRAFT')
 ) v(Name,FollowUp,Status)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.ExamSessions e WHERE e.CreatedBy=@lecturer AND e.ExamName=v.Name);

 INSERT dbo.ExamSchedules(ExamID,StudentID,AllocatedStartTime,AllocatedEndTime,Status)
 SELECT e.ExamID,u.UserID,e.StartTime,e.EndTime,'NOT_STARTED'
 FROM dbo.ExamSessions e CROSS JOIN dbo.Users u
 JOIN dbo.Roles r ON r.RoleID=u.RoleID
 WHERE e.CreatedBy=@lecturer AND e.ExamName=N'[DEMO] Thi thử SWD392 - không cần AI'
 AND u.Username IN ('demo_student01','demo_student02','demo_student03')
 AND r.RoleName='STUDENT' AND u.user_status='ACTIVE'
 AND NOT EXISTS(SELECT 1 FROM dbo.ExamSchedules s WHERE s.ExamID=e.ExamID AND s.StudentID=u.UserID);

 COMMIT TRANSACTION;
 SELECT u.Username,r.RoleName,u.FullName,u.user_status
 FROM dbo.Users u JOIN dbo.Roles r ON r.RoleID=u.RoleID WHERE u.Username LIKE 'demo[_]%';
 SELECT e.ExamID,e.ExamName,e.Status,e.StartTime,e.EndTime FROM dbo.ExamSessions e
 WHERE e.CreatedBy=@lecturer AND e.ExamName LIKE N'[[]DEMO]%';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
