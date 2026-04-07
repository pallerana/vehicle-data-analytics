IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        first_name NVARCHAR(100) NOT NULL,
        last_name NVARCHAR(100) NOT NULL,
        email NVARCHAR(255) NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NULL
    );

    CREATE UNIQUE INDEX UX_users_email ON dbo.users(email);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.users)
BEGIN
    INSERT INTO dbo.users (first_name, last_name, email, created_at, updated_at)
    VALUES
        ('Rana', 'Khan', 'rana.khan@solera.example', SYSUTCDATETIME(), SYSUTCDATETIME()),
        ('Ava', 'Mitchell', 'ava.mitchell@solera.example', SYSUTCDATETIME(), SYSUTCDATETIME()),
        ('Noah', 'Bennett', 'noah.bennett@solera.example', SYSUTCDATETIME(), SYSUTCDATETIME()),
        ('Priya', 'Sharma', 'priya.sharma@solera.example', SYSUTCDATETIME(), SYSUTCDATETIME()),
        ('Lucas', 'Meyer', 'lucas.meyer@solera.example', SYSUTCDATETIME(), SYSUTCDATETIME());
END;
