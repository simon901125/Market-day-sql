USE MarketDayDB;
GO

/* =========================================================
   Clear all stored data in MarketDayDB.

   This script keeps tables, indexes, constraints, triggers,
   and extended properties. It only deletes table rows and
   reseeds identity columns back to 0.
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
    DELETE FROM dbo.event_applications;
    DELETE FROM dbo.event_stalls;
    DELETE FROM dbo.event_equipments;
    DELETE FROM dbo.event_stall_zones;
    DELETE FROM dbo.event_images;
    DELETE FROM dbo.event_unpublish_requests;
    DELETE FROM dbo.market_events;
    DELETE FROM dbo.vendor_products;
    DELETE FROM dbo.vendor_images;
    DELETE FROM dbo.organizer_profiles;
    DELETE FROM dbo.vendor_profiles;
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
    DBCC CHECKIDENT ('dbo.event_applications', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_stalls', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_equipments', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_stall_zones', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_images', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.event_unpublish_requests', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.market_events', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.vendor_products', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.vendor_images', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.organizer_profiles', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.vendor_profiles', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.user_profiles', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.user_tokens', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.notifications', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.admin_operation_logs', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.request_logs', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.users', RESEED, 0) WITH NO_INFOMSGS;
    DBCC CHECKIDENT ('dbo.categories', RESEED, 0) WITH NO_INFOMSGS;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO
