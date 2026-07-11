USE master;
GO

/* Drops the whole MarketDayDB database, so new tables do not need
   separate DROP TABLE entries here. */
IF DB_ID(N'MarketDayDB') IS NOT NULL
BEGIN
    SELECT
        s.session_id,
        s.login_name,
        s.host_name,
        s.program_name,
        s.status,
        r.command,
        r.status AS request_status
    FROM sys.dm_exec_sessions AS s
    LEFT JOIN sys.dm_exec_requests AS r
        ON s.session_id = r.session_id
    WHERE s.database_id = DB_ID(N'MarketDayDB')
       OR r.database_id = DB_ID(N'MarketDayDB');

    ALTER DATABASE MarketDayDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE MarketDayDB;
END
GO
