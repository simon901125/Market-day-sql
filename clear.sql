USE MarketDayDB;
GO

/* =========================================================
   Clear all stored data in MarketDayDB, including test2~test5
   seed data such as MD*, T2-*, MD0101, and T5-* rows.

   This script keeps tables, indexes, constraints, triggers,
   and extended properties. It deletes business/test rows,
   resets identities, then restores the default admin account
   and the seven system categories.
   ========================================================= */

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    /* Child tables first to satisfy foreign key constraints. */
    DELETE FROM dbo.status_logs;
    DELETE FROM dbo.refunds;
    DELETE FROM dbo.payments;
    DELETE FROM dbo.rental_appliances;
    DELETE FROM dbo.equipment_rentals;
    DELETE FROM dbo.application_dates;
    DELETE FROM dbo.application_review_notes;
    DELETE FROM dbo.event_applications;
    DELETE FROM dbo.event_stalls;
    DELETE FROM dbo.event_equipments;
    DELETE FROM dbo.event_stall_zones;
    DELETE FROM dbo.event_unpublish_requests;
    DELETE FROM dbo.market_events;
    DELETE FROM dbo.vendor_products;
    DELETE FROM dbo.organizer_profiles;
    DELETE FROM dbo.vendor_profiles;
    DELETE FROM dbo.admin_profiles;
    DELETE FROM dbo.user_profiles;
    DELETE FROM dbo.user_tokens;
    DELETE FROM dbo.notifications;
    DELETE FROM dbo.admin_operation_logs;
    DELETE FROM dbo.request_logs;
    DELETE FROM dbo.users;
    DELETE FROM dbo.categories;

    /* Reset identity values so the next inserted row starts at 1. */
    DBCC CHECKIDENT ('dbo.status_logs', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.refunds', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.payments', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.rental_appliances', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.equipment_rentals', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.application_dates', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.application_review_notes', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_applications', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_stalls', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_equipments', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_stall_zones', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_unpublish_requests', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.market_events', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.vendor_products', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.organizer_profiles', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.vendor_profiles', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.admin_profiles', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.user_profiles', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.user_tokens', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.notifications', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.admin_operation_logs', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.request_logs', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.users', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.categories', RESEED, 0) WITH NO_INFOMSGS;

    /* Restore the default administrator account. Password: a12345678 */
    INSERT INTO dbo.users (
        role,
        email,
        password_hash,
        provider,
        status,
        isLogin,
        email_verified_at
    )
    VALUES (
        'ADMIN',
        'admin@marketday.local',
        '$2a$10$mbBnbPGQHnFgV4OnLqsMVO2ootBD99oXGZPZr6RIMLNsWlvwy5.rC',
        'LOCAL',
        'ACTIVE',
        0,
        SYSDATETIME()
    );

    INSERT INTO dbo.admin_profiles (user_id, admin_name)
    SELECT id, N'系統管理員'
    FROM dbo.users
    WHERE email = 'admin@marketday.local';

    /* Restore the fixed category options required by the application. */
    INSERT INTO dbo.categories (name, slug, is_active)
    VALUES
        (N'餐飲美食', N'food', 1),
        (N'文創手作', N'handmade', 1),
        (N'親子家庭', N'family', 1),
        (N'寵物生活', N'pet-life', 1),
        (N'植物選物', N'plants', 1),
        (N'服飾配件', N'fashion-accessories', 1),
        (N'玩具選物', N'toys', 1);

    COMMIT TRANSACTION;

    SELECT
        (SELECT COUNT(*) FROM dbo.users) AS users,
        (SELECT COUNT(*) FROM dbo.admin_profiles) AS adminProfiles,
        (SELECT COUNT(*) FROM dbo.categories) AS categories,
        (SELECT COUNT(*) FROM dbo.market_events) AS marketEvents,
        (SELECT COUNT(*) FROM dbo.event_applications) AS eventApplications,
        (SELECT COUNT(*) FROM dbo.application_review_notes) AS applicationReviewNotes,
        (SELECT COUNT(*) FROM dbo.payments) AS payments,
        (SELECT COUNT(*) FROM dbo.refunds) AS refunds;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO
