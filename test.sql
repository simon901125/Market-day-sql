USE MarketDayDB;
GO

/* =========================================================
   Test seed:
   - 1 organizer creates 1 event
   - 2 vendor profiles each create 1 application for the event
   - applications are approved and paid, but not assigned stalls
   ========================================================= */

DECLARE @test_password_hash VARCHAR(255) = '$2a$10$SEC249Wk1nnIL/bxrXtxNeMCrP0jmBilegHXJy3HMLvs4nksiUlz.';

IF NOT EXISTS (
    SELECT 1 FROM dbo.users WHERE email = 'organizer@example.test'
)
BEGIN
    INSERT INTO dbo.users (
        role,
        name,
        email,
        password_hash,
        phone,
        provider,
        status,
        email_verified_at
    )
    VALUES (
        'ORGANIZER',
        N'Test Organizer',
        'organizer@example.test',
        @test_password_hash,
        '+886900000001',
        'LOCAL',
        'ACTIVE',
        SYSDATETIME()
    );
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.users WHERE email = 'vendor-one@example.test'
)
BEGIN
    INSERT INTO dbo.users (
        role,
        name,
        email,
        password_hash,
        phone,
        provider,
        status,
        email_verified_at
    )
    VALUES (
        'VENDOR',
        N'Test Vendor One',
        'vendor-one@example.test',
        @test_password_hash,
        '+886900000002',
        'LOCAL',
        'ACTIVE',
        SYSDATETIME()
    );
END;

IF NOT EXISTS (
    SELECT 1 FROM dbo.users WHERE email = 'vendor-two@example.test'
)
BEGIN
    INSERT INTO dbo.users (
        role,
        name,
        email,
        password_hash,
        phone,
        provider,
        status,
        email_verified_at
    )
    VALUES (
        'VENDOR',
        N'Test Vendor Two',
        'vendor-two@example.test',
        @test_password_hash,
        '+886900000003',
        'LOCAL',
        'ACTIVE',
        SYSDATETIME()
    );
END;

UPDATE dbo.users
SET password_hash = @test_password_hash,
    isLogin = 0,
    updated_at = SYSDATETIME()
WHERE id = 1;

UPDATE dbo.users
SET password_hash = @test_password_hash,
    isLogin = 0,
    updated_at = SYSDATETIME()
WHERE id = 2;

UPDATE dbo.users
SET password_hash = @test_password_hash,
    isLogin = 0,
    updated_at = SYSDATETIME()
WHERE id = 3;

IF NOT EXISTS (
    SELECT 1 FROM dbo.categories WHERE slug = N'food'
)
BEGIN
    INSERT INTO dbo.categories (
        name,
        slug,
        is_active
    )
    VALUES (
        N'餐飲美食',
        N'food',
        1
    );
END;

DECLARE @organizer_user_id BIGINT = (
    SELECT id FROM dbo.users WHERE email = 'organizer@example.test'
);

DECLARE @vendor_one_user_id BIGINT = (
    SELECT id FROM dbo.users WHERE email = 'vendor-one@example.test'
);

DECLARE @vendor_two_user_id BIGINT = (
    SELECT id FROM dbo.users WHERE email = 'vendor-two@example.test'
);

DECLARE @category_id BIGINT = (
    SELECT id FROM dbo.categories WHERE slug = N'food'
);

IF NOT EXISTS (
    SELECT 1 FROM dbo.market_events WHERE title = N'ABC 分區攤位測試活動'
)
BEGIN
    INSERT INTO dbo.market_events (
        user_id,
        category_id,
        title,
        summary,
        description,
        location_name,
        city,
        district,
        address,
        traffic_info,
        notice,
        start_date,
        end_date,
        start_time,
        end_time,
        registration_start_at,
        registration_end_at,
        max_booths,
        base_fee,
        cover_image_url,
        map_image_url,
        public_info_at,
        review_status,
        publish_status
    )
    VALUES (
        @organizer_user_id,
        @category_id,
        N'ABC 分區攤位測試活動',
        N'用於測試攤位選擇流程的活動，包含 A、B、C 三個分區。',
        N'此測試活動提供 A、B、C 三個攤位分區，每區 5 個攤位，可用於實作攤位查詢、選取與狀態更新流程。',
        N'小集日測試廣場',
        N'台北市',
        N'信義區',
        N'台北市信義區市府路1號',
        N'捷運市政府站步行約 5 分鐘。',
        N'此為攤位選擇功能測試資料。',
        CONVERT(date, '2026-08-01'),
        CONVERT(date, '2026-08-02'),
        CONVERT(time(0), '11:00'),
        CONVERT(time(0), '19:00'),
        CONVERT(datetime2(0), '2026-07-01 09:00:00'),
        CONVERT(datetime2(0), '2026-07-20 18:00:00'),
        15,
        CONVERT(decimal(10,2), 1000),
        NULL,
        N'/uploads/marketEvent/底圖.jpg',
        CONVERT(datetime2(0), '2026-07-01 10:00:00'),
        N'APPROVED',
        N'PUBLISHED'
    );
END;

DECLARE @event_id BIGINT = (
    SELECT id FROM dbo.market_events WHERE title = N'ABC 分區攤位測試活動'
);

INSERT INTO dbo.event_sessions (
    event_id,
    session_date,
    start_time,
    end_time,
    max_booths
)
SELECT
    @event_id,
    v.session_date,
    CONVERT(time(0), '11:00'),
    CONVERT(time(0), '19:00'),
    15
FROM (
    VALUES
        (CONVERT(date, '2026-08-01')),
        (CONVERT(date, '2026-08-02'))
) AS v (session_date)
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.event_sessions s
    WHERE s.event_id = @event_id
      AND s.session_date = v.session_date
);

INSERT INTO dbo.event_stall_zones (
    event_id,
    zone_name,
    stall_count
)
SELECT
    @event_id,
    v.zone_name,
    v.stall_count
FROM (
    VALUES
        (N'A', 5),
        (N'B', 5),
        (N'C', 5)
) AS v (zone_name, stall_count)
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.event_stall_zones z
    WHERE z.event_id = @event_id
      AND z.zone_name = v.zone_name
);

INSERT INTO dbo.event_stalls (
    event_id,
    zone_id,
    stall_no,
    width,
    length,
    height,
    status
)
SELECT
    @event_id,
    z.id,
    v.stall_no,
    CONVERT(decimal(6,2), 3.00),
    CONVERT(decimal(6,2), 3.00),
    CONVERT(decimal(6,2), 2.50),
    N'AVAILABLE'
FROM (
    VALUES
        (N'A', N'A01'),
        (N'A', N'A02'),
        (N'A', N'A03'),
        (N'A', N'A04'),
        (N'A', N'A05'),
        (N'B', N'B01'),
        (N'B', N'B02'),
        (N'B', N'B03'),
        (N'B', N'B04'),
        (N'B', N'B05'),
        (N'C', N'C01'),
        (N'C', N'C02'),
        (N'C', N'C03'),
        (N'C', N'C04'),
        (N'C', N'C05')
) AS v (zone_name, stall_no)
INNER JOIN dbo.event_stall_zones z
    ON z.event_id = @event_id
   AND z.zone_name = v.zone_name
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.event_stalls s
    WHERE s.event_id = @event_id
      AND s.stall_no = v.stall_no
);

IF NOT EXISTS (
    SELECT 1
    FROM dbo.user_profiles
    WHERE user_id = @vendor_one_user_id
      AND profile_type = N'VENDOR'
      AND name = N'Test Vendor One 品牌'
)
BEGIN
    INSERT INTO dbo.user_profiles (
        user_id,
        profile_type,
        name,
        contact_name,
        contact_phone,
        contact_email,
        city,
        district,
        address
    )
    VALUES (
        @vendor_one_user_id,
        N'VENDOR',
        N'Test Vendor One 品牌',
        N'Test Vendor One',
        N'+886900000002',
        N'vendor-one@example.test',
        N'台北市',
        N'信義區',
        N'台北市信義區市府路1號'
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.user_profiles
    WHERE user_id = @vendor_two_user_id
      AND profile_type = N'VENDOR'
      AND name = N'Test Vendor Two 品牌'
)
BEGIN
    INSERT INTO dbo.user_profiles (
        user_id,
        profile_type,
        name,
        contact_name,
        contact_phone,
        contact_email,
        city,
        district,
        address
    )
    VALUES (
        @vendor_two_user_id,
        N'VENDOR',
        N'Test Vendor Two 品牌',
        N'Test Vendor Two',
        N'+886900000003',
        N'vendor-two@example.test',
        N'台北市',
        N'信義區',
        N'台北市信義區市府路1號'
    );
END;

DECLARE @vendor_one_user_profile_id BIGINT = (
    SELECT id
    FROM dbo.user_profiles
    WHERE user_id = @vendor_one_user_id
      AND profile_type = N'VENDOR'
      AND name = N'Test Vendor One 品牌'
);

DECLARE @vendor_two_user_profile_id BIGINT = (
    SELECT id
    FROM dbo.user_profiles
    WHERE user_id = @vendor_two_user_id
      AND profile_type = N'VENDOR'
      AND name = N'Test Vendor Two 品牌'
);

IF NOT EXISTS (
    SELECT 1
    FROM dbo.vendor_profiles
    WHERE user_profile_id = @vendor_one_user_profile_id
)
BEGIN
    INSERT INTO dbo.vendor_profiles (
        user_profile_id,
        category_id,
        brand_description,
        product_summary
    )
    VALUES (
        @vendor_one_user_profile_id,
        @category_id,
        N'此品牌用於測試 ABC 分區攤位活動的報名與選位流程。',
        N'第一個測試攤商品牌。'
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.vendor_profiles
    WHERE user_profile_id = @vendor_two_user_profile_id
)
BEGIN
    INSERT INTO dbo.vendor_profiles (
        user_profile_id,
        category_id,
        brand_description,
        product_summary
    )
    VALUES (
        @vendor_two_user_profile_id,
        @category_id,
        N'此品牌用於測試 ABC 分區攤位活動的報名與選位流程。',
        N'第二個測試攤商品牌。'
    );
END;

DECLARE @vendor_one_profile_id BIGINT = (
    SELECT id
    FROM dbo.vendor_profiles
    WHERE user_profile_id = @vendor_one_user_profile_id
);

DECLARE @vendor_two_profile_id BIGINT = (
    SELECT id
    FROM dbo.vendor_profiles
    WHERE user_profile_id = @vendor_two_user_profile_id
);

IF NOT EXISTS (
    SELECT 1
    FROM dbo.event_applications
    WHERE application_no = N'MD001'
)
BEGIN
    INSERT INTO dbo.event_applications (
        application_no,
        event_id,
        user_id,
        vendor_profile_id,
        vehicle_no,
        applicant_note,
        total_amount,
        deposit_amount,
        deposit_status,
        payment_due_at,
        review_status,
        review_note,
        payment_status
    )
    VALUES (
        N'MD001',
        @event_id,
        @vendor_one_user_id,
        @vendor_one_profile_id,
        N'ABC-0001',
        N'Test Vendor One 的測試申請表，尚未選擇攤位。',
        CONVERT(decimal(10,2), 1000),
        CONVERT(decimal(10,2), 0),
        N'NOT_REFUNDED',
        CONVERT(datetime2(0), '2026-07-25 18:00:00'),
        N'APPROVED',
        NULL,
        N'PAID'
    );
END;

IF NOT EXISTS (
    SELECT 1
    FROM dbo.event_applications
    WHERE application_no = N'MD002'
)
BEGIN
    INSERT INTO dbo.event_applications (
        application_no,
        event_id,
        user_id,
        vendor_profile_id,
        vehicle_no,
        applicant_note,
        total_amount,
        deposit_amount,
        deposit_status,
        payment_due_at,
        review_status,
        review_note,
        payment_status
    )
    VALUES (
        N'MD002',
        @event_id,
        @vendor_two_user_id,
        @vendor_two_profile_id,
        N'ABC-0002',
        N'Test Vendor Two 的測試申請表，尚未選擇攤位。',
        CONVERT(decimal(10,2), 1000),
        CONVERT(decimal(10,2), 0),
        N'NOT_REFUNDED',
        CONVERT(datetime2(0), '2026-07-25 18:00:00'),
        N'APPROVED',
        NULL,
        N'PAID'
    );
END;
