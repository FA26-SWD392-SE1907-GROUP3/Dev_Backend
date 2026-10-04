-- Run before starting the updated backend. Preserves existing accounts.
USE AIVES;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.Users','GoogleSubject') IS NULL
 ALTER TABLE dbo.Users ADD GoogleSubject varchar(255) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.Users') AND name='UQ_Users_GoogleSubject')
 EXEC(N'CREATE UNIQUE INDEX UQ_Users_GoogleSubject ON dbo.Users(GoogleSubject) WHERE GoogleSubject IS NOT NULL');
COMMIT TRANSACTION;
