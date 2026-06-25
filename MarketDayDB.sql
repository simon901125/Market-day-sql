IF DB_ID(N'MarketDayDB') IS NULL
BEGIN
    CREATE DATABASE MarketDayDB;
END
GO

USE MarketDayDB;
GO

/* =========================================================
   小集日 Market Day - MVP SQL Server Database
   Source: 資料庫規劃.md
   ========================================================= */

CREATE TABLE dbo.users
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    role VARCHAR(30) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    phone VARCHAR(30) NULL,
    provider VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL CONSTRAINT DF_users_status DEFAULT 'UNACTIVE',
    isLogin BIT NOT NULL CONSTRAINT DF_users_isLogin DEFAULT 0,
    email_verified_at DATETIME2(0) NULL,
    expired_time DATETIME2(0) NOT NULL CONSTRAINT DF_users_expired_time DEFAULT SYSDATETIME(),
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_users_updated_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT CK_users_role CHECK (role IN ('VENDOR', 'ORGANIZER', 'ADMIN')),
    CONSTRAINT CK_users_provider CHECK (provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT CK_users_status CHECK (status IN ('UNACTIVE', 'ACTIVE', 'IS_DELETED'))
);
GO

CREATE INDEX IX_users_role_status ON dbo.users(role, status);
CREATE INDEX IX_users_provider ON dbo.users(provider);
CREATE INDEX IX_users_isLogin_expired_time ON dbo.users(isLogin, expired_time);
GO

CREATE TRIGGER dbo.trg_users_activate_after_email_verified
ON dbo.users
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE users
    SET status = 'ACTIVE'
    FROM dbo.users users
        INNER JOIN inserted inserted_users ON users.id = inserted_users.id
    WHERE inserted_users.email_verified_at IS NOT NULL
        AND users.status = 'UNACTIVE';
END;
GO

CREATE TABLE dbo.user_tokens
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL,
    token_type VARCHAR(30) NOT NULL,
    expires_at DATETIME2(0) NOT NULL,
    CONSTRAINT PK_user_tokens PRIMARY KEY (id),
    CONSTRAINT UQ_user_tokens_token UNIQUE (token),
    CONSTRAINT FK_user_tokens_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT CK_user_tokens_type CHECK (token_type IN ('EMAIL_VERIFY', 'PASSWORD_RESET'))
);
GO

CREATE INDEX IX_user_tokens_user_type ON dbo.user_tokens(user_id, token_type);
CREATE INDEX IX_user_tokens_type_expires ON dbo.user_tokens(token_type, expires_at);
GO

CREATE TABLE dbo.categories
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    name NVARCHAR(100) NOT NULL,
    slug NVARCHAR(100) NOT NULL,
    is_active BIT NOT NULL CONSTRAINT DF_categories_is_active DEFAULT 1,
    CONSTRAINT PK_categories PRIMARY KEY (id),
    CONSTRAINT UQ_categories_slug UNIQUE (slug)
);
GO

CREATE TABLE dbo.user_profiles
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    profile_type NVARCHAR(30) NOT NULL,
    name NVARCHAR(150) NOT NULL,
    contact_name NVARCHAR(100) NOT NULL,
    contact_phone NVARCHAR(30) NOT NULL,
    contact_email NVARCHAR(255) NULL,
    city NVARCHAR(50) NULL,
    district NVARCHAR(50) NULL,
    address NVARCHAR(255) NULL,
    CONSTRAINT PK_user_profiles PRIMARY KEY (id),
    CONSTRAINT FK_user_profiles_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT CK_user_profiles_profile_type CHECK (profile_type IN (N'VENDOR', N'ORGANIZER'))
);
GO

CREATE INDEX IX_user_profiles_user_profile_type ON dbo.user_profiles(user_id, profile_type);
GO

CREATE TABLE dbo.vendor_profiles
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_profile_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    instagram_url NVARCHAR(500) NULL,
    facebook_url NVARCHAR(500) NULL,
    website_url NVARCHAR(500) NULL,
    brand_description NVARCHAR(MAX) NULL,
    brand_type NVARCHAR(100) NULL,
    product_summary NVARCHAR(MAX) NULL,
    CONSTRAINT PK_vendor_profiles PRIMARY KEY (id),
    CONSTRAINT UQ_vendor_profiles_user_profile UNIQUE (user_profile_id),
    CONSTRAINT FK_vendor_profiles_user_profiles FOREIGN KEY (user_profile_id) REFERENCES dbo.user_profiles(id),
    CONSTRAINT FK_vendor_profiles_categories FOREIGN KEY (category_id) REFERENCES dbo.categories(id)
);
GO

CREATE INDEX IX_vendor_profiles_category ON dbo.vendor_profiles(category_id);
GO

CREATE TABLE dbo.organizer_profiles
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_profile_id BIGINT NOT NULL,
    company_name NVARCHAR(150) NULL,
    tax_id NVARCHAR(20) NULL,
    service_days NVARCHAR(100) NULL,
    service_start_time TIME(0) NULL,
    service_end_time TIME(0) NULL,
    CONSTRAINT PK_organizer_profiles PRIMARY KEY (id),
    CONSTRAINT UQ_organizer_profiles_user_profile UNIQUE (user_profile_id),
    CONSTRAINT FK_organizer_profiles_user_profiles FOREIGN KEY (user_profile_id) REFERENCES dbo.user_profiles(id)
);
GO

CREATE TRIGGER dbo.trg_vendor_profiles_validate_profile_type
ON dbo.vendor_profiles
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN dbo.user_profiles up ON up.id = i.user_profile_id
        WHERE up.profile_type <> N'VENDOR'
    )
    BEGIN
        THROW 50001, N'vendor_profiles.user_profile_id must reference a VENDOR profile.', 1;
    END
END
GO

CREATE TRIGGER dbo.trg_organizer_profiles_validate_profile_type
ON dbo.organizer_profiles
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN dbo.user_profiles up ON up.id = i.user_profile_id
        WHERE up.profile_type <> N'ORGANIZER'
    )
    BEGIN
        THROW 50002, N'organizer_profiles.user_profile_id must reference an ORGANIZER profile.', 1;
    END
END
GO

CREATE TABLE dbo.vendor_images
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    vendor_profile_id BIGINT NOT NULL,
    image_type NVARCHAR(30) NOT NULL,
    image_url NVARCHAR(500) NOT NULL,
    CONSTRAINT PK_vendor_images PRIMARY KEY (id),
    CONSTRAINT FK_vendor_images_vendor_profiles FOREIGN KEY (vendor_profile_id) REFERENCES dbo.vendor_profiles(id),
    CONSTRAINT CK_vendor_images_image_type CHECK (image_type IN (N'AVATAR', N'COVER', N'GALLERY'))
);
GO

CREATE INDEX IX_vendor_images_vendor_profile_type ON dbo.vendor_images(vendor_profile_id, image_type);
CREATE UNIQUE INDEX UQ_vendor_images_avatar ON dbo.vendor_images(vendor_profile_id) WHERE image_type = N'AVATAR';
CREATE UNIQUE INDEX UQ_vendor_images_cover ON dbo.vendor_images(vendor_profile_id) WHERE image_type = N'COVER';
GO

CREATE TABLE dbo.vendor_products
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    vendor_profile_id BIGINT NOT NULL,
    name NVARCHAR(150) NOT NULL,
    short_description NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    price DECIMAL(10,2) NULL,
    image_url NVARCHAR(500) NULL,
    status NVARCHAR(30) NOT NULL,
    CONSTRAINT PK_vendor_products PRIMARY KEY (id),
    CONSTRAINT FK_vendor_products_vendor_profiles FOREIGN KEY (vendor_profile_id) REFERENCES dbo.vendor_profiles(id),
    CONSTRAINT CK_vendor_products_status CHECK (status IN (N'ACTIVE', N'HIDDEN'))
);
GO

CREATE TABLE dbo.market_events
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    summary NVARCHAR(300) NOT NULL,
    description NVARCHAR(MAX) NOT NULL,
    location_name NVARCHAR(200) NOT NULL,
    city NVARCHAR(50) NOT NULL,
    district NVARCHAR(50) NULL,
    address NVARCHAR(255) NOT NULL,
    traffic_info NVARCHAR(MAX) NULL,
    notice NVARCHAR(MAX) NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME(0) NULL,
    end_time TIME(0) NULL,
    registration_start_at DATETIME2(0) NOT NULL,
    registration_end_at DATETIME2(0) NOT NULL,
    max_booths INT NOT NULL,
    base_fee DECIMAL(10,2) NOT NULL,
    cover_image_url NVARCHAR(500) NULL,
    map_image_url NVARCHAR(500) NULL,
    public_info_at DATETIME2(0) NULL,
    review_status NVARCHAR(30) NOT NULL CONSTRAINT DF_market_events_review_status DEFAULT N'REVISION_REQUIRED',
    review_note NVARCHAR(MAX) NULL,
    publish_status NVARCHAR(30) NOT NULL CONSTRAINT DF_market_events_publish_status DEFAULT N'DRAFT',
    CONSTRAINT PK_market_events PRIMARY KEY (id),
    CONSTRAINT FK_market_events_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_market_events_categories FOREIGN KEY (category_id) REFERENCES dbo.categories(id),
    CONSTRAINT CK_market_events_date_range CHECK (end_date >= start_date),
    CONSTRAINT CK_market_events_registration_range CHECK (registration_end_at >= registration_start_at),
    CONSTRAINT CK_market_events_review_status CHECK (review_status IN (N'APPROVED', N'REJECTED', N'REVISION_REQUIRED')),
    CONSTRAINT CK_market_events_publish_status CHECK (publish_status IN (N'DRAFT', N'PUBLISHED', N'UNPUBLISHED', N'CANCELLED'))
);
GO

CREATE INDEX IX_market_events_user ON dbo.market_events(user_id);
CREATE INDEX IX_market_events_dates ON dbo.market_events(start_date, end_date);
CREATE INDEX IX_market_events_city_category ON dbo.market_events(city, category_id);
CREATE INDEX IX_market_events_review_status ON dbo.market_events(review_status);
CREATE INDEX IX_market_events_publish_status ON dbo.market_events(publish_status);
GO

CREATE TABLE dbo.event_images
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    image_url NVARCHAR(500) NOT NULL,
    CONSTRAINT PK_event_images PRIMARY KEY (id),
    CONSTRAINT FK_event_images_market_events FOREIGN KEY (event_id) REFERENCES dbo.market_events(id)
);
GO

CREATE TABLE dbo.event_stall_zones
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    zone_name NVARCHAR(50) NOT NULL,
    stall_count INT NOT NULL,
    CONSTRAINT PK_event_stall_zones PRIMARY KEY (id),
    CONSTRAINT FK_event_stall_zones_market_events FOREIGN KEY (event_id) REFERENCES dbo.market_events(id),
    CONSTRAINT CK_event_stall_zones_stall_count CHECK (stall_count >= 0)
);
GO

CREATE INDEX IX_event_stall_zones_event ON dbo.event_stall_zones(event_id);
GO

CREATE TABLE dbo.event_stalls
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    event_id BIGINT NOT NULL,
    zone_id BIGINT NOT NULL,
    stall_no NVARCHAR(30) NOT NULL,
    width DECIMAL(6,2) NULL,
    length DECIMAL(6,2) NULL,
    height DECIMAL(6,2) NULL,
    status NVARCHAR(30) NOT NULL CONSTRAINT DF_event_stalls_status DEFAULT N'AVAILABLE',
    CONSTRAINT PK_event_stalls PRIMARY KEY (id),
    CONSTRAINT UQ_event_stalls_event_stall_no UNIQUE (event_id, stall_no),
    CONSTRAINT FK_event_stalls_market_events FOREIGN KEY (event_id) REFERENCES dbo.market_events(id),
    CONSTRAINT FK_event_stalls_event_stall_zones FOREIGN KEY (zone_id) REFERENCES dbo.event_stall_zones(id),
    CONSTRAINT CK_event_stalls_status CHECK (status IN (N'AVAILABLE', N'SELECTED', N'SOLD', N'DISABLED'))
);
GO

CREATE INDEX IX_event_stalls_event_status ON dbo.event_stalls(event_id, status);
CREATE INDEX IX_event_stalls_zone ON dbo.event_stalls(zone_id);
GO

CREATE TABLE dbo.event_applications
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    application_no NVARCHAR(30) NOT NULL,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    vendor_profile_id BIGINT NOT NULL,
    selected_stall_id BIGINT NULL,
    vehicle_no NVARCHAR(30) NULL,
    applicant_note NVARCHAR(MAX) NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL CONSTRAINT DF_event_applications_deposit_amount DEFAULT 0,
    deposit_status NVARCHAR(30) NOT NULL CONSTRAINT DF_event_applications_deposit_status DEFAULT N'NOT_RETURNED',
    payment_due_at DATETIME2(0) NULL,
    review_status NVARCHAR(30) NOT NULL CONSTRAINT DF_event_applications_review_status DEFAULT N'PENDING',
    review_note NVARCHAR(MAX) NULL,
    payment_status NVARCHAR(30) NOT NULL CONSTRAINT DF_event_applications_payment_status DEFAULT N'PENDING',
    is_cancelled BIT NOT NULL CONSTRAINT DF_event_applications_is_cancelled DEFAULT 0,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_event_applications_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_event_applications PRIMARY KEY (id),
    CONSTRAINT UQ_event_applications_application_no UNIQUE (application_no),
    CONSTRAINT UQ_event_applications_event_vendor_profile UNIQUE (event_id, vendor_profile_id),
    CONSTRAINT FK_event_applications_market_events FOREIGN KEY (event_id) REFERENCES dbo.market_events(id),
    CONSTRAINT FK_event_applications_users FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    CONSTRAINT FK_event_applications_vendor_profiles FOREIGN KEY (vendor_profile_id) REFERENCES dbo.vendor_profiles(id),
    CONSTRAINT FK_event_applications_event_stalls FOREIGN KEY (selected_stall_id) REFERENCES dbo.event_stalls(id),
    CONSTRAINT CK_event_applications_review_status CHECK (review_status IN (N'PENDING', N'APPROVED', N'REJECTED')),
    CONSTRAINT CK_event_applications_deposit_status CHECK (deposit_status IN (N'NOT_RETURNED', N'RETURNED')),
    CONSTRAINT CK_event_applications_payment_status CHECK (payment_status IN (N'PENDING', N'PAID', N'FAILED', N'EXPIRED'))
);
GO

CREATE INDEX IX_event_applications_event_review ON dbo.event_applications(event_id, review_status);
CREATE INDEX IX_event_applications_user ON dbo.event_applications(user_id);
CREATE INDEX IX_event_applications_selected_stall ON dbo.event_applications(selected_stall_id);
CREATE INDEX IX_event_applications_payment_due ON dbo.event_applications(payment_status, payment_due_at);
CREATE INDEX IX_event_applications_cancelled ON dbo.event_applications(is_cancelled);
CREATE INDEX IX_event_applications_created ON dbo.event_applications(created_at);
GO

CREATE TABLE dbo.application_dates
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    application_id BIGINT NOT NULL,
    apply_date DATE NOT NULL,
    CONSTRAINT PK_application_dates PRIMARY KEY (id),
    CONSTRAINT UQ_application_dates_application_date UNIQUE (application_id, apply_date),
    CONSTRAINT FK_application_dates_event_applications FOREIGN KEY (application_id) REFERENCES dbo.event_applications(id)
);
GO

CREATE INDEX IX_application_dates_apply_date ON dbo.application_dates(apply_date);
GO

CREATE TABLE dbo.payments
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    payment_no NVARCHAR(40) NOT NULL,
    application_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    provider NVARCHAR(30) NULL,
    provider_trade_no NVARCHAR(100) NULL,
    status NVARCHAR(30) NOT NULL,
    paid_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_payments_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_payments PRIMARY KEY (id),
    CONSTRAINT UQ_payments_payment_no UNIQUE (payment_no),
    CONSTRAINT FK_payments_event_applications FOREIGN KEY (application_id) REFERENCES dbo.event_applications(id),
    CONSTRAINT CK_payments_status CHECK (status IN (N'PENDING', N'PAID', N'FAILED', N'EXPIRED'))
);
GO

CREATE INDEX IX_payments_application ON dbo.payments(application_id);
CREATE INDEX IX_payments_status ON dbo.payments(status);
GO

CREATE TABLE dbo.refunds
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    refund_no NVARCHAR(40) NOT NULL,
    application_id BIGINT NOT NULL,
    payment_id BIGINT NULL,
    amount DECIMAL(10,2) NOT NULL,
    refund_status NVARCHAR(30) NOT NULL CONSTRAINT DF_refunds_refund_status DEFAULT N'REFUND_REQUESTED',
    refunded_at DATETIME2(0) NULL,
    CONSTRAINT PK_refunds PRIMARY KEY (id),
    CONSTRAINT UQ_refunds_refund_no UNIQUE (refund_no),
    CONSTRAINT FK_refunds_event_applications FOREIGN KEY (application_id) REFERENCES dbo.event_applications(id),
    CONSTRAINT FK_refunds_payments FOREIGN KEY (payment_id) REFERENCES dbo.payments(id),
    CONSTRAINT CK_refunds_refund_status CHECK (refund_status IN (N'REFUND_REQUESTED', N'REFUNDING', N'REFUND_FAILED', N'REFUNDED'))
);
GO

CREATE INDEX IX_refunds_application ON dbo.refunds(application_id);
CREATE INDEX IX_refunds_payment ON dbo.refunds(payment_id);
CREATE INDEX IX_refunds_refund_status ON dbo.refunds(refund_status);
GO

CREATE TABLE dbo.notifications
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    type NVARCHAR(50) NOT NULL,
    title NVARCHAR(150) NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    CONSTRAINT PK_notifications PRIMARY KEY (id),
    CONSTRAINT FK_notifications_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

CREATE INDEX IX_notifications_user ON dbo.notifications(user_id);
GO

CREATE TABLE dbo.request_logs
(
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NULL,
    method NVARCHAR(10) NOT NULL,
    path NVARCHAR(500) NOT NULL,
    status_code INT NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_request_logs_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_request_logs PRIMARY KEY (id),
    CONSTRAINT FK_request_logs_users FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);
GO

CREATE INDEX IX_request_logs_user_created ON dbo.request_logs(user_id, created_at);
CREATE INDEX IX_request_logs_path_created ON dbo.request_logs(path, created_at);
GO

/* =========================================================
   Column descriptions
   ========================================================= */

CREATE OR ALTER PROCEDURE dbo.usp_add_table_description
    @table_name SYSNAME,
    @description NVARCHAR(4000)
AS
BEGIN
    IF EXISTS (
        SELECT 1
    FROM sys.extended_properties ep
        INNER JOIN sys.tables t ON ep.major_id = t.object_id
        INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
    WHERE ep.name = N'MS_Description'
        AND s.name = N'dbo'
        AND t.name = @table_name
        AND ep.minor_id = 0
    )
    BEGIN
        EXEC sys.sp_updateextendedproperty
            @name = N'MS_Description',
            @value = @description,
            @level0type = N'SCHEMA', @level0name = N'dbo',
            @level1type = N'TABLE',  @level1name = @table_name;
    END
    ELSE
    BEGIN
        EXEC sys.sp_addextendedproperty
            @name = N'MS_Description',
            @value = @description,
            @level0type = N'SCHEMA', @level0name = N'dbo',
            @level1type = N'TABLE',  @level1name = @table_name;
    END
END
GO

CREATE OR ALTER PROCEDURE dbo.usp_add_column_description
    @table_name SYSNAME,
    @column_name SYSNAME,
    @description NVARCHAR(4000)
AS
BEGIN
    IF EXISTS (
        SELECT 1
    FROM sys.extended_properties ep
        INNER JOIN sys.tables t ON ep.major_id = t.object_id
        INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
        INNER JOIN sys.columns c ON ep.major_id = c.object_id AND ep.minor_id = c.column_id
    WHERE ep.name = N'MS_Description'
        AND s.name = N'dbo'
        AND t.name = @table_name
        AND c.name = @column_name
    )
    BEGIN
        EXEC sys.sp_updateextendedproperty
            @name = N'MS_Description',
            @value = @description,
            @level0type = N'SCHEMA', @level0name = N'dbo',
            @level1type = N'TABLE',  @level1name = @table_name,
            @level2type = N'COLUMN', @level2name = @column_name;
    END
    ELSE
    BEGIN
        EXEC sys.sp_addextendedproperty
            @name = N'MS_Description',
            @value = @description,
            @level0type = N'SCHEMA', @level0name = N'dbo',
            @level1type = N'TABLE',  @level1name = @table_name,
            @level2type = N'COLUMN', @level2name = @column_name;
    END
END
GO

EXEC dbo.usp_add_column_description N'users', N'id', N'使用者主鍵';
EXEC dbo.usp_add_column_description N'users', N'role', N'帳號角色（VENDOR/ORGANIZER/ADMIN）';
EXEC dbo.usp_add_column_description N'users', N'name', N'顯示名稱';
EXEC dbo.usp_add_column_description N'users', N'email', N'登入 Email';
EXEC dbo.usp_add_column_description N'users', N'password_hash', N'密碼雜湊值（LOCAL）';
EXEC dbo.usp_add_column_description N'users', N'phone', N'聯絡電話';
EXEC dbo.usp_add_column_description N'users', N'provider', N'登入來源（LOCAL/GOOGLE）';
EXEC dbo.usp_add_column_description N'users', N'status', N'帳號狀態（UNACTIVE/ACTIVE/IS_DELETED）';
EXEC dbo.usp_add_column_description N'users', N'isLogin', N'是否已有登入中的裝置';
EXEC dbo.usp_add_column_description N'users', N'email_verified_at', N'Email 驗證完成時間';
EXEC dbo.usp_add_column_description N'users', N'created_at', N'建立時間';
EXEC dbo.usp_add_column_description N'users', N'expired_time', N'自動登出判斷時間';
EXEC dbo.usp_add_column_description N'users', N'updated_at', N'更新時間';

EXEC dbo.usp_add_column_description N'user_tokens', N'id', N'使用者 token ID';
EXEC dbo.usp_add_column_description N'user_tokens', N'user_id', N'使用者 ID';
EXEC dbo.usp_add_column_description N'user_tokens', N'token', N'token';
EXEC dbo.usp_add_column_description N'user_tokens', N'token_type', N'token 類型（EMAIL_VERIFY/PASSWORD_RESET）';
EXEC dbo.usp_add_column_description N'user_tokens', N'expires_at', N'過期時間';

EXEC dbo.usp_add_column_description N'categories', N'id', N'分類 ID';
EXEC dbo.usp_add_column_description N'categories', N'name', N'分類名稱';
EXEC dbo.usp_add_column_description N'categories', N'slug', N'分類代碼';
EXEC dbo.usp_add_column_description N'categories', N'is_active', N'是否啟用';

EXEC dbo.usp_add_column_description N'user_profiles', N'id', N'個人資料 ID';
EXEC dbo.usp_add_column_description N'user_profiles', N'user_id', N'使用者 ID';
EXEC dbo.usp_add_column_description N'user_profiles', N'profile_type', N'資料類型（VENDOR/ORGANIZER）';
EXEC dbo.usp_add_column_description N'user_profiles', N'name', N'名稱';
EXEC dbo.usp_add_column_description N'user_profiles', N'contact_name', N'聯絡人';
EXEC dbo.usp_add_column_description N'user_profiles', N'contact_phone', N'聯絡電話';
EXEC dbo.usp_add_column_description N'user_profiles', N'contact_email', N'聯絡 Email';
EXEC dbo.usp_add_column_description N'user_profiles', N'city', N'縣市';
EXEC dbo.usp_add_column_description N'user_profiles', N'district', N'區';
EXEC dbo.usp_add_column_description N'user_profiles', N'address', N'詳細地址';

EXEC dbo.usp_add_column_description N'vendor_profiles', N'id', N'攤主品牌資料 ID';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'user_profile_id', N'共用個人資料 ID';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'category_id', N'品牌分類 ID';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'instagram_url', N'Instagram';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'facebook_url', N'Facebook';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'website_url', N'官方網站';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'brand_description', N'品牌資訊';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'brand_type', N'品牌類型';
EXEC dbo.usp_add_column_description N'vendor_profiles', N'product_summary', N'品牌商品摘要';

EXEC dbo.usp_add_column_description N'organizer_profiles', N'id', N'主辦方資料 ID';
EXEC dbo.usp_add_column_description N'organizer_profiles', N'user_profile_id', N'共用個人資料 ID';
EXEC dbo.usp_add_column_description N'organizer_profiles', N'company_name', N'公司名稱';
EXEC dbo.usp_add_column_description N'organizer_profiles', N'tax_id', N'統一編號';
EXEC dbo.usp_add_column_description N'organizer_profiles', N'service_days', N'服務星期';
EXEC dbo.usp_add_column_description N'organizer_profiles', N'service_start_time', N'服務開始時間';
EXEC dbo.usp_add_column_description N'organizer_profiles', N'service_end_time', N'服務結束時間';

EXEC dbo.usp_add_column_description N'vendor_images', N'id', N'照片 ID';
EXEC dbo.usp_add_column_description N'vendor_images', N'vendor_profile_id', N'攤主品牌資料 ID';
EXEC dbo.usp_add_column_description N'vendor_images', N'image_type', N'圖片類型';
EXEC dbo.usp_add_column_description N'vendor_images', N'image_url', N'圖片路徑';

EXEC dbo.usp_add_column_description N'vendor_products', N'id', N'商品 ID';
EXEC dbo.usp_add_column_description N'vendor_products', N'vendor_profile_id', N'攤主品牌資料 ID';
EXEC dbo.usp_add_column_description N'vendor_products', N'name', N'商品名稱';
EXEC dbo.usp_add_column_description N'vendor_products', N'short_description', N'商品簡介';
EXEC dbo.usp_add_column_description N'vendor_products', N'description', N'商品介紹';
EXEC dbo.usp_add_column_description N'vendor_products', N'price', N'商品價格';
EXEC dbo.usp_add_column_description N'vendor_products', N'image_url', N'商品圖片';
EXEC dbo.usp_add_column_description N'vendor_products', N'status', N'商品狀態';

EXEC dbo.usp_add_column_description N'market_events', N'id', N'活動 ID';
EXEC dbo.usp_add_column_description N'market_events', N'user_id', N'主辦方 ID';
EXEC dbo.usp_add_column_description N'market_events', N'category_id', N'活動分類';
EXEC dbo.usp_add_column_description N'market_events', N'title', N'活動名稱';
EXEC dbo.usp_add_column_description N'market_events', N'summary', N'活動摘要';
EXEC dbo.usp_add_column_description N'market_events', N'description', N'活動介紹';
EXEC dbo.usp_add_column_description N'market_events', N'location_name', N'地點名稱';
EXEC dbo.usp_add_column_description N'market_events', N'city', N'縣市';
EXEC dbo.usp_add_column_description N'market_events', N'district', N'區域';
EXEC dbo.usp_add_column_description N'market_events', N'address', N'地址';
EXEC dbo.usp_add_column_description N'market_events', N'traffic_info', N'交通方式';
EXEC dbo.usp_add_column_description N'market_events', N'notice', N'活動注意事項';
EXEC dbo.usp_add_column_description N'market_events', N'start_date', N'活動開始日';
EXEC dbo.usp_add_column_description N'market_events', N'end_date', N'活動結束日';
EXEC dbo.usp_add_column_description N'market_events', N'start_time', N'每日開始時間';
EXEC dbo.usp_add_column_description N'market_events', N'end_time', N'每日結束時間';
EXEC dbo.usp_add_column_description N'market_events', N'registration_start_at', N'報名開始時間';
EXEC dbo.usp_add_column_description N'market_events', N'registration_end_at', N'報名截止時間';
EXEC dbo.usp_add_column_description N'market_events', N'max_booths', N'攤位總數';
EXEC dbo.usp_add_column_description N'market_events', N'base_fee', N'基本攤位費';
EXEC dbo.usp_add_column_description N'market_events', N'cover_image_url', N'活動封面';
EXEC dbo.usp_add_column_description N'market_events', N'map_image_url', N'攤位地圖底圖';
EXEC dbo.usp_add_column_description N'market_events', N'public_info_at', N'公開資訊時間';
EXEC dbo.usp_add_column_description N'market_events', N'review_status', N'活動審核狀態（APPROVED/REJECTED/REVISION_REQUIRED/CANCELLED）';
EXEC dbo.usp_add_column_description N'market_events', N'review_note', N'補件原因 / 審核備註';
EXEC dbo.usp_add_column_description N'market_events', N'publish_status', N'活動發布狀態（DRAFT/PUBLISHED/UNPUBLISHED/CANCELLED）';

EXEC dbo.usp_add_column_description N'event_images', N'id', N'圖片 ID';
EXEC dbo.usp_add_column_description N'event_images', N'event_id', N'活動 ID';
EXEC dbo.usp_add_column_description N'event_images', N'image_url', N'圖片路徑';

EXEC dbo.usp_add_column_description N'event_stall_zones', N'id', N'分區 ID';
EXEC dbo.usp_add_column_description N'event_stall_zones', N'event_id', N'活動 ID';
EXEC dbo.usp_add_column_description N'event_stall_zones', N'zone_name', N'分區名稱';
EXEC dbo.usp_add_column_description N'event_stall_zones', N'stall_count', N'分區攤位數量';

EXEC dbo.usp_add_column_description N'event_stalls', N'id', N'攤位 ID';
EXEC dbo.usp_add_column_description N'event_stalls', N'event_id', N'活動 ID';
EXEC dbo.usp_add_column_description N'event_stalls', N'zone_id', N'分區 ID';
EXEC dbo.usp_add_column_description N'event_stalls', N'stall_no', N'攤位編號';
EXEC dbo.usp_add_column_description N'event_stalls', N'width', N'攤位寬度';
EXEC dbo.usp_add_column_description N'event_stalls', N'length', N'攤位長度';
EXEC dbo.usp_add_column_description N'event_stalls', N'height', N'攤位高度';
EXEC dbo.usp_add_column_description N'event_stalls', N'status', N'攤位狀態（AVAILABLE/SELECTED/SOLD/DISABLED）';

EXEC dbo.usp_add_column_description N'event_applications', N'id', N'報名 ID';
EXEC dbo.usp_add_column_description N'event_applications', N'application_no', N'報名編號';
EXEC dbo.usp_add_column_description N'event_applications', N'event_id', N'活動 ID';
EXEC dbo.usp_add_column_description N'event_applications', N'user_id', N'攤主 ID';
EXEC dbo.usp_add_column_description N'event_applications', N'vendor_profile_id', N'攤主品牌資料 ID';
EXEC dbo.usp_add_column_description N'event_applications', N'selected_stall_id', N'選擇的活動攤位';
EXEC dbo.usp_add_column_description N'event_applications', N'vehicle_no', N'車牌';
EXEC dbo.usp_add_column_description N'event_applications', N'applicant_note', N'攤主備註';
EXEC dbo.usp_add_column_description N'event_applications', N'total_amount', N'試算總金額';
EXEC dbo.usp_add_column_description N'event_applications', N'deposit_amount', N'保證金金額';
EXEC dbo.usp_add_column_description N'event_applications', N'deposit_status', N'保證金退還狀態（NOT_RETURNED/RETURNED）';
EXEC dbo.usp_add_column_description N'event_applications', N'payment_due_at', N'付款期限';
EXEC dbo.usp_add_column_description N'event_applications', N'review_status', N'報名審核狀態（PENDING/APPROVED/REJECTED）';
EXEC dbo.usp_add_column_description N'event_applications', N'review_note', N'報名審核未通過原因';
EXEC dbo.usp_add_column_description N'event_applications', N'payment_status', N'報名付款狀態（PENDING/PAID/FAILED/EXPIRED）';
EXEC dbo.usp_add_column_description N'event_applications', N'is_cancelled', N'報名是否取消';
EXEC dbo.usp_add_column_description N'event_applications', N'created_at', N'報名建立時間';

EXEC dbo.usp_add_column_description N'application_dates', N'id', N'ID';
EXEC dbo.usp_add_column_description N'application_dates', N'application_id', N'報名 ID';
EXEC dbo.usp_add_column_description N'application_dates', N'apply_date', N'報名參加日期';

EXEC dbo.usp_add_column_description N'payments', N'id', N'付款 ID';
EXEC dbo.usp_add_column_description N'payments', N'payment_no', N'付款編號';
EXEC dbo.usp_add_column_description N'payments', N'application_id', N'報名 ID';
EXEC dbo.usp_add_column_description N'payments', N'amount', N'付款金額';
EXEC dbo.usp_add_column_description N'payments', N'provider', N'金流服務商';
EXEC dbo.usp_add_column_description N'payments', N'provider_trade_no', N'金流交易編號';
EXEC dbo.usp_add_column_description N'payments', N'status', N'付款狀態（PENDING/PAID/FAILED/EXPIRED）';
EXEC dbo.usp_add_column_description N'payments', N'paid_at', N'付款成功時間';
EXEC dbo.usp_add_column_description N'payments', N'created_at', N'建立時間';

EXEC dbo.usp_add_column_description N'refunds', N'id', N'退款 ID';
EXEC dbo.usp_add_column_description N'refunds', N'refund_no', N'退款編號';
EXEC dbo.usp_add_column_description N'refunds', N'application_id', N'報名 ID';
EXEC dbo.usp_add_column_description N'refunds', N'payment_id', N'原付款紀錄';
EXEC dbo.usp_add_column_description N'refunds', N'amount', N'退款金額';
EXEC dbo.usp_add_column_description N'refunds', N'refund_status', N'退款處理狀態（REFUND_REQUESTED/REFUNDING/REFUND_FAILED/REFUNDED）';
EXEC dbo.usp_add_column_description N'refunds', N'refunded_at', N'實際退款完成時間';

EXEC dbo.usp_add_column_description N'notifications', N'id', N'通知 ID';
EXEC dbo.usp_add_column_description N'notifications', N'user_id', N'接收者';
EXEC dbo.usp_add_column_description N'notifications', N'type', N'通知類型';
EXEC dbo.usp_add_column_description N'notifications', N'title', N'通知標題';
EXEC dbo.usp_add_column_description N'notifications', N'content', N'通知內容';

EXEC dbo.usp_add_column_description N'request_logs', N'id', N'請求紀錄 ID';
EXEC dbo.usp_add_column_description N'request_logs', N'user_id', N'發送請求者';
EXEC dbo.usp_add_column_description N'request_logs', N'method', N'HTTP 方法';
EXEC dbo.usp_add_column_description N'request_logs', N'path', N'API 路徑';
EXEC dbo.usp_add_column_description N'request_logs', N'status_code', N'回應狀態碼';
EXEC dbo.usp_add_column_description N'request_logs', N'created_at', N'建立時間';
GO

DROP PROCEDURE dbo.usp_add_column_description;
GO

DROP PROCEDURE dbo.usp_add_table_description;
GO
