-- Run AFTER 01-create-aives.sql on the existing AIVES database.
-- Updates existing values without deleting data. Can be run again.
USE AIVES;
GO
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID('dbo.CK_Documents_Status','C') IS NOT NULL ALTER TABLE dbo.SubjectDocuments DROP CONSTRAINT CK_Documents_Status;
 IF OBJECT_ID('dbo.CK_Questions_Source','C') IS NOT NULL ALTER TABLE dbo.Questions DROP CONSTRAINT CK_Questions_Source;
 IF OBJECT_ID('dbo.CK_Questions_Status','C') IS NOT NULL ALTER TABLE dbo.Questions DROP CONSTRAINT CK_Questions_Status;
 IF OBJECT_ID('dbo.CK_Exams_Status','C') IS NOT NULL ALTER TABLE dbo.ExamSessions DROP CONSTRAINT CK_Exams_Status;
 IF OBJECT_ID('dbo.CK_Schedules_Status','C') IS NOT NULL ALTER TABLE dbo.ExamSchedules DROP CONSTRAINT CK_Schedules_Status;
 IF OBJECT_ID('dbo.CK_Logs_Type','C') IS NOT NULL ALTER TABLE dbo.InterviewLogs DROP CONSTRAINT CK_Logs_Type;
 IF OBJECT_ID('dbo.DF_Documents_Status','D') IS NOT NULL ALTER TABLE dbo.SubjectDocuments DROP CONSTRAINT DF_Documents_Status;
 IF OBJECT_ID('dbo.DF_Exams_Status','D') IS NOT NULL ALTER TABLE dbo.ExamSessions DROP CONSTRAINT DF_Exams_Status;
 IF OBJECT_ID('dbo.DF_Schedules_Status','D') IS NOT NULL ALTER TABLE dbo.ExamSchedules DROP CONSTRAINT DF_Schedules_Status;
 IF OBJECT_ID('dbo.UQ_Roles_RoleName','UQ') IS NOT NULL ALTER TABLE dbo.Roles DROP CONSTRAINT UQ_Roles_RoleName;
 IF EXISTS(SELECT 1 FROM sys.indexes WHERE name='IX_Questions_SubjectStatus' AND object_id=OBJECT_ID('dbo.Questions')) DROP INDEX IX_Questions_SubjectStatus ON dbo.Questions;
 ALTER TABLE dbo.Roles ALTER COLUMN RoleName varchar(50) NOT NULL;
 ALTER TABLE dbo.Roles ADD CONSTRAINT UQ_Roles_RoleName UNIQUE(RoleName);
 UPDATE dbo.Questions SET BloomLevel=CASE BloomLevel WHEN N'Nhớ' THEN 'REMEMBER' WHEN N'Hiểu' THEN 'UNDERSTAND' WHEN N'Vận dụng' THEN 'APPLY' WHEN N'Phân tích' THEN 'ANALYZE' ELSE UPPER(BloomLevel) END;
 ALTER TABLE dbo.Questions ALTER COLUMN BloomLevel varchar(50) NOT NULL;
 ALTER TABLE dbo.Questions ALTER COLUMN Source varchar(50) NOT NULL;
 ALTER TABLE dbo.Questions ALTER COLUMN Status varchar(50) NOT NULL;
 ALTER TABLE dbo.ExamSessions ALTER COLUMN Status varchar(20) NULL;
 ALTER TABLE dbo.ExamSchedules ALTER COLUMN Status varchar(50) NULL;
 ALTER TABLE dbo.InterviewLogs ALTER COLUMN QuestionType varchar(20) NOT NULL;
 UPDATE dbo.Roles SET RoleName=UPPER(RoleName);
 UPDATE dbo.SubjectDocuments SET Status=UPPER(Status);
 UPDATE dbo.Questions SET Source=UPPER(Source), Status=UPPER(Status), BloomLevel=CASE BloomLevel
   WHEN N'Nhớ' THEN 'REMEMBER' WHEN N'Hiểu' THEN 'UNDERSTAND' WHEN N'Vận dụng' THEN 'APPLY' WHEN N'Phân tích' THEN 'ANALYZE' ELSE UPPER(BloomLevel) END;
 UPDATE dbo.ExamSessions SET Status=UPPER(Status);
 UPDATE dbo.ExamSchedules SET Status=UPPER(Status);
 UPDATE dbo.InterviewLogs SET QuestionType=UPPER(QuestionType);
 ALTER TABLE dbo.SubjectDocuments ADD CONSTRAINT DF_Documents_Status DEFAULT N'PROCESSING' FOR Status;
 ALTER TABLE dbo.ExamSessions ADD CONSTRAINT DF_Exams_Status DEFAULT 'DRAFT' FOR Status;
 ALTER TABLE dbo.ExamSchedules ADD CONSTRAINT DF_Schedules_Status DEFAULT 'NOT_STARTED' FOR Status;
 ALTER TABLE dbo.SubjectDocuments ADD CONSTRAINT CK_Documents_Status CHECK(Status IN ('PROCESSING','INDEXED','ERROR'));
 ALTER TABLE dbo.Questions ADD CONSTRAINT CK_Questions_Source CHECK(Source IN ('MANUAL','AI_RAG'));
 ALTER TABLE dbo.Questions ADD CONSTRAINT CK_Questions_Status CHECK(Status IN ('PENDING_APPROVAL','APPROVED','REJECTED'));
 ALTER TABLE dbo.ExamSessions ADD CONSTRAINT CK_Exams_Status CHECK(Status IN ('DRAFT','PUBLISHED','COMPLETED'));
 ALTER TABLE dbo.ExamSchedules ADD CONSTRAINT CK_Schedules_Status CHECK(Status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED','MISSED'));
 ALTER TABLE dbo.InterviewLogs ADD CONSTRAINT CK_Logs_Type CHECK(QuestionType IN ('MAIN','FOLLOW_UP'));
 CREATE INDEX IX_Questions_SubjectStatus ON dbo.Questions(SubjectID,Status);
 IF COL_LENGTH('dbo.SubjectDocuments','ExtractedText') IS NULL ALTER TABLE dbo.SubjectDocuments ADD ExtractedText nvarchar(max) NULL;
 IF COL_LENGTH('dbo.Questions','ApprovedBy') IS NULL ALTER TABLE dbo.Questions ADD ApprovedBy int NULL CONSTRAINT FK_Questions_Approver REFERENCES dbo.Users(UserID);
 IF COL_LENGTH('dbo.Questions','ApprovedAt') IS NULL ALTER TABLE dbo.Questions ADD ApprovedAt datetime2(6) NULL;
 IF COL_LENGTH('dbo.ExamSessions','AnswerTimeLimitSeconds') IS NULL ALTER TABLE dbo.ExamSessions ADD AnswerTimeLimitSeconds int NOT NULL CONSTRAINT DF_Exams_AnswerLimit DEFAULT 120 CONSTRAINT CK_Exams_AnswerLimit CHECK(AnswerTimeLimitSeconds BETWEEN 15 AND 1800);
 IF COL_LENGTH('dbo.InterviewLogs','ParentLogID') IS NULL ALTER TABLE dbo.InterviewLogs ADD ParentLogID int NULL CONSTRAINT FK_Logs_Parent REFERENCES dbo.InterviewLogs(LogID);
 IF COL_LENGTH('dbo.InterviewLogs','SequenceNumber') IS NULL
 BEGIN
  ALTER TABLE dbo.InterviewLogs ADD SequenceNumber int NULL;
  EXEC(N'WITH numbered AS (SELECT LogID,ROW_NUMBER() OVER(PARTITION BY ScheduleID ORDER BY AskedAt,LogID) AS seq FROM dbo.InterviewLogs) UPDATE l SET SequenceNumber=n.seq FROM dbo.InterviewLogs l JOIN numbered n ON n.LogID=l.LogID');
  EXEC(N'ALTER TABLE dbo.InterviewLogs ALTER COLUMN SequenceNumber int NOT NULL');
 END;
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='UQ_Logs_Sequence' AND object_id=OBJECT_ID('dbo.InterviewLogs')) EXEC(N'CREATE UNIQUE INDEX UQ_Logs_Sequence ON dbo.InterviewLogs(ScheduleID,SequenceNumber)');
 -- Existing documents must be reindexed from the original local files before RAG.
 EXEC(N'UPDATE dbo.SubjectDocuments SET Status=''PROCESSING'' WHERE ExtractedText IS NULL AND Status=''INDEXED''');
 COMMIT TRANSACTION;
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;
