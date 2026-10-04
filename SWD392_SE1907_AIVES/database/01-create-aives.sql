-- Run in SQL Server Management Studio (SSMS).
-- Initial schema only: stops if any application table already exists.
USE master;
GO
IF DB_ID(N'AIVES') IS NULL
    EXEC(N'CREATE DATABASE [AIVES]');
GO
USE AIVES;
GO
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.Roles', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.Users', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.Subjects', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.SubjectDocuments', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.Questions', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.Rubrics', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.ExamSessions', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.ExamSchedules', N'U') IS NOT NULL
 OR OBJECT_ID(N'dbo.InterviewLogs', N'U') IS NOT NULL
    THROW 50001, 'Application tables already exist. Use a migration instead of this initial script.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    CREATE TABLE dbo.Roles (
        RoleID int IDENTITY(1,1) CONSTRAINT PK_Roles PRIMARY KEY,
        RoleName nvarchar(50) NOT NULL CONSTRAINT UQ_Roles_RoleName UNIQUE,
        Description nvarchar(255) NULL
    );

    CREATE TABLE dbo.Users (
        UserID int IDENTITY(1,1) CONSTRAINT PK_Users PRIMARY KEY,
        RoleID int NOT NULL,
        Username nvarchar(50) NOT NULL CONSTRAINT UQ_Users_Username UNIQUE,
        PasswordHash nvarchar(255) NOT NULL,
        FullName nvarchar(100) NOT NULL,
        Email nvarchar(100) NOT NULL CONSTRAINT UQ_Users_Email UNIQUE,
        CreatedAt datetime2(6) NULL CONSTRAINT DF_Users_CreatedAt DEFAULT SYSDATETIME(),
        user_status varchar(255) NOT NULL CONSTRAINT DF_Users_Status DEFAULT 'ACTIVE',
        CONSTRAINT FK_Users_Roles FOREIGN KEY (RoleID) REFERENCES dbo.Roles(RoleID),
        CONSTRAINT CK_Users_Status CHECK (user_status IN ('ACTIVE','INACTIVE'))
    );

    CREATE TABLE dbo.Subjects (
        SubjectID int IDENTITY(1,1) CONSTRAINT PK_Subjects PRIMARY KEY,
        SubjectCode nvarchar(20) NOT NULL CONSTRAINT UQ_Subjects_Code UNIQUE,
        SubjectName nvarchar(100) NOT NULL,
        Description nvarchar(max) NULL
    );

    CREATE TABLE dbo.SubjectDocuments (
        DocumentID int IDENTITY(1,1) CONSTRAINT PK_SubjectDocuments PRIMARY KEY,
        SubjectID int NOT NULL,
        UploadedBy int NOT NULL,
        Title nvarchar(200) NOT NULL,
        FilePath nvarchar(500) NOT NULL,
        FileType nvarchar(20) NULL,
        Status nvarchar(30) NULL CONSTRAINT DF_Documents_Status DEFAULT N'Indexed',
        UploadedAt datetime2(6) NULL CONSTRAINT DF_Documents_UploadedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Documents_Subjects FOREIGN KEY (SubjectID) REFERENCES dbo.Subjects(SubjectID),
        CONSTRAINT FK_Documents_Users FOREIGN KEY (UploadedBy) REFERENCES dbo.Users(UserID),
        CONSTRAINT CK_Documents_Status CHECK (Status IN (N'Processing',N'Indexed',N'Error'))
    );

    CREATE TABLE dbo.Questions (
        QuestionID int IDENTITY(1,1) CONSTRAINT PK_Questions PRIMARY KEY,
        SubjectID int NOT NULL,
        DocumentID int NULL,
        CreatedBy int NOT NULL,
        Topic nvarchar(150) NULL,
        QuestionContent nvarchar(max) NOT NULL,
        BloomLevel nvarchar(50) NOT NULL,
        Source nvarchar(50) NOT NULL,
        Status nvarchar(50) NOT NULL,
        CreatedAt datetime2(6) NULL CONSTRAINT DF_Questions_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Questions_Subjects FOREIGN KEY (SubjectID) REFERENCES dbo.Subjects(SubjectID),
        CONSTRAINT FK_Questions_Documents FOREIGN KEY (DocumentID) REFERENCES dbo.SubjectDocuments(DocumentID),
        CONSTRAINT FK_Questions_Users FOREIGN KEY (CreatedBy) REFERENCES dbo.Users(UserID),
        CONSTRAINT CK_Questions_Source CHECK (Source IN (N'Manual',N'AI_RAG')),
        CONSTRAINT CK_Questions_Status CHECK (Status IN (N'Pending_Approval',N'Approved',N'Rejected'))
    );

    CREATE TABLE dbo.Rubrics (
        RubricID int IDENTITY(1,1) CONSTRAINT PK_Rubrics PRIMARY KEY,
        QuestionID int NOT NULL,
        CriteriaName nvarchar(255) NOT NULL,
        MaxScore decimal(5,2) NOT NULL,
        Guideline nvarchar(max) NULL,
        CreatedAt datetime2(6) NULL CONSTRAINT DF_Rubrics_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Rubrics_Questions FOREIGN KEY (QuestionID) REFERENCES dbo.Questions(QuestionID),
        CONSTRAINT CK_Rubrics_Score CHECK (MaxScore > 0)
    );

    CREATE TABLE dbo.ExamSessions (
        ExamID int IDENTITY(1,1) CONSTRAINT PK_ExamSessions PRIMARY KEY,
        SubjectID int NOT NULL,
        CreatedBy int NOT NULL,
        ExamName nvarchar(200) NOT NULL,
        StartTime datetime2(6) NOT NULL,
        EndTime datetime2(6) NOT NULL,
        MaxMainQuestions int NOT NULL,
        MaxFollowUpQuestions int NOT NULL,
        Status nvarchar(20) NULL CONSTRAINT DF_Exams_Status DEFAULT N'Draft',
        CONSTRAINT FK_Exams_Subjects FOREIGN KEY (SubjectID) REFERENCES dbo.Subjects(SubjectID),
        CONSTRAINT FK_Exams_Users FOREIGN KEY (CreatedBy) REFERENCES dbo.Users(UserID),
        CONSTRAINT CK_Exams_Time CHECK (EndTime > StartTime),
        CONSTRAINT CK_Exams_Main CHECK (MaxMainQuestions > 0),
        CONSTRAINT CK_Exams_FollowUp CHECK (MaxFollowUpQuestions >= 0),
        CONSTRAINT CK_Exams_Status CHECK (Status IN (N'Draft',N'Published',N'Completed'))
    );

    CREATE TABLE dbo.ExamSchedules (
        ScheduleID int IDENTITY(1,1) CONSTRAINT PK_ExamSchedules PRIMARY KEY,
        ExamID int NOT NULL,
        StudentID int NOT NULL,
        AllocatedStartTime datetime2(6) NULL,
        AllocatedEndTime datetime2(6) NULL,
        ActualStartTime datetime2(6) NULL,
        ActualEndTime datetime2(6) NULL,
        Status nvarchar(50) NULL CONSTRAINT DF_Schedules_Status DEFAULT N'Not_Started',
        CONSTRAINT FK_Schedules_Exams FOREIGN KEY (ExamID) REFERENCES dbo.ExamSessions(ExamID),
        CONSTRAINT FK_Schedules_Users FOREIGN KEY (StudentID) REFERENCES dbo.Users(UserID),
        CONSTRAINT UQ_Schedules_ExamStudent UNIQUE (ExamID,StudentID),
        CONSTRAINT CK_Schedules_AllocatedTime CHECK (AllocatedEndTime > AllocatedStartTime),
        CONSTRAINT CK_Schedules_ActualTime CHECK (ActualEndTime >= ActualStartTime),
        CONSTRAINT CK_Schedules_Status CHECK (Status IN (N'Not_Started',N'In_Progress',N'Completed',N'Missed'))
    );

    CREATE TABLE dbo.InterviewLogs (
        LogID int IDENTITY(1,1) CONSTRAINT PK_InterviewLogs PRIMARY KEY,
        ScheduleID int NOT NULL,
        QuestionID int NULL,
        QuestionContent nvarchar(max) NOT NULL,
        QuestionType nvarchar(20) NOT NULL,
        QuestionAudioURL nvarchar(500) NULL,
        StudentAnswerTranscript nvarchar(max) NULL,
        AnswerAudioURL nvarchar(500) NULL,
        AskedAt datetime2(6) NOT NULL,
        AnsweredAt datetime2(6) NULL,
        TimeTakenSeconds int NULL,
        CONSTRAINT FK_Logs_Schedules FOREIGN KEY (ScheduleID) REFERENCES dbo.ExamSchedules(ScheduleID),
        CONSTRAINT FK_Logs_Questions FOREIGN KEY (QuestionID) REFERENCES dbo.Questions(QuestionID),
        CONSTRAINT CK_Logs_Type CHECK (QuestionType IN (N'Main',N'Follow_Up')),
        CONSTRAINT CK_Logs_Time CHECK (AnsweredAt >= AskedAt),
        CONSTRAINT CK_Logs_Duration CHECK (TimeTakenSeconds >= 0)
    );

    CREATE INDEX IX_Users_RoleID ON dbo.Users(RoleID);
    CREATE INDEX IX_Documents_SubjectID ON dbo.SubjectDocuments(SubjectID);
    CREATE INDEX IX_Documents_UploadedBy ON dbo.SubjectDocuments(UploadedBy);
    CREATE INDEX IX_Questions_SubjectStatus ON dbo.Questions(SubjectID, Status);
    CREATE INDEX IX_Questions_DocumentID ON dbo.Questions(DocumentID);
    CREATE INDEX IX_Questions_CreatedBy ON dbo.Questions(CreatedBy);
    CREATE INDEX IX_Rubrics_QuestionID ON dbo.Rubrics(QuestionID);
    CREATE INDEX IX_Exams_SubjectID ON dbo.ExamSessions(SubjectID);
    CREATE INDEX IX_Exams_CreatedBy ON dbo.ExamSessions(CreatedBy);
    CREATE INDEX IX_Schedules_StudentID ON dbo.ExamSchedules(StudentID);
    CREATE INDEX IX_Logs_ScheduleAskedAt ON dbo.InterviewLogs(ScheduleID, AskedAt);
    CREATE INDEX IX_Logs_QuestionID ON dbo.InterviewLogs(QuestionID);

    INSERT INTO dbo.Roles(RoleName, Description) VALUES
        (N'Admin', N'Quản trị viên'),
        (N'Lecturer', N'Giảng viên'),
        (N'Student', N'Sinh viên');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
