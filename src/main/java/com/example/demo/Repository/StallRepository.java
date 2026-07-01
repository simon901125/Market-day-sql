package com.example.demo.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StallRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<Long> findStallId(Long eventId, String stallNo) {
        String sql = """
                SELECT id
                FROM dbo.event_stalls
                WHERE event_id = :eventId
                  AND stall_no = :stallNo
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("stallNo", stallNo);

        List<Long> stallIds = namedParameterJdbcTemplate.queryForList(sql, map, Long.class);
        return stallIds.stream().findFirst();
    }

    public Optional<Map<String, Object>> findStallForSelection(Long eventId, String stallNo) {
        String sql = """
                SELECT
                    id,
                    event_id AS eventId,
                    stall_no AS stallNo,
                    status
                FROM dbo.event_stalls
                WHERE event_id = :eventId
                  AND stall_no = :stallNo
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("stallNo", stallNo);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public Optional<Map<String, Object>> findApplicationForSelection(String applicationNo) {
        String sql = """
                SELECT
                    id,
                    application_no AS applicationNo,
                    event_id AS eventId,
                    user_id AS userId,
                    vendor_profile_id AS vendorProfileId,
                    selected_stall_id AS selectedStallId,
                    review_status AS reviewStatus,
                    payment_status AS paymentStatus,
                    is_cancelled AS isCancelled
                FROM dbo.event_applications
                WHERE application_no = :applicationNo
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("applicationNo", applicationNo);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public Optional<Map<String, Object>> findSelectableApplication(String applicationNo) {
        String sql = """
                SELECT
                    id,
                    application_no AS applicationNo,
                    event_id AS eventId,
                    user_id AS userId,
                    vendor_profile_id AS vendorProfileId,
                    selected_stall_id AS selectedStallId,
                    review_status AS reviewStatus,
                    payment_status AS paymentStatus
                FROM dbo.event_applications
                WHERE application_no = :applicationNo
                  AND review_status = N'APPROVED'
                  AND payment_status = N'PAID'
                  AND selected_stall_id IS NULL
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("applicationNo", applicationNo);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public Optional<Map<String, Object>> findVendorAccountByEmail(String email) {
        String sql = """
                SELECT
                    u.id AS userId,
                    u.role,
                    u.email,
                    u.provider,
                    up.id AS userProfileId,
                    vp.id AS vendorProfileId,
                    up.name,
                    up.contact_name AS contactName,
                    up.contact_phone AS contactPhone,
                    up.contact_email AS contactEmail,
                    up.city,
                    up.district,
                    up.address,
                    vp.category_id AS categoryId,
                    c.name AS categoryName,
                    c.slug AS categorySlug,
                    vp.instagram_url AS instagramUrl,
                    vp.facebook_url AS facebookUrl,
                    vp.website_url AS websiteUrl,
                    vp.brand_description AS brandDescription,
                    vp.brand_type AS brandType,
                    vp.product_summary AS productSummary
                FROM dbo.users u
                INNER JOIN dbo.user_profiles up ON up.user_id = u.id
                    AND up.profile_type = N'VENDOR'
                INNER JOIN dbo.vendor_profiles vp ON vp.user_profile_id = up.id
                INNER JOIN dbo.categories c ON c.id = vp.category_id
                WHERE u.email = :email
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("email", email);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public int bindApplicationSelectedStall(Long eventId, String applicationNo, Long stallId) {
        String sql = """
                UPDATE dbo.event_applications
                SET selected_stall_id = :stallId
                WHERE event_id = :eventId
                  AND application_no = :applicationNo
                  AND review_status = N'APPROVED'
                  AND payment_status = N'PAID'
                  AND selected_stall_id IS NULL
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("applicationNo", applicationNo);
        map.put("stallId", stallId);
        return namedParameterJdbcTemplate.update(sql, map);
    }

    public int selectAvailableStall(Long stallId) {
        String sql = """
                UPDATE dbo.event_stalls
                SET status = N'SELECTED'
                WHERE id = :stallId
                  AND status = N'AVAILABLE'
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("stallId", stallId);
        return namedParameterJdbcTemplate.update(sql, map);
    }

    public Optional<Map<String, Object>> findSelectedStallApplication(String applicationNo, String stallNo) {
        String sql = """
                SELECT
                    a.id AS applicationId,
                    a.selected_stall_id AS selectedStallId,
                    s.status AS stallStatus
                FROM dbo.event_applications a
                INNER JOIN dbo.event_stalls s ON s.id = a.selected_stall_id
                WHERE a.application_no = :applicationNo
                  AND s.stall_no = :stallNo
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("applicationNo", applicationNo);
        map.put("stallNo", stallNo);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public Optional<Map<String, Object>> findVendorStallMapApplication(String applicationNo) {
        String sql = """
                SELECT
                    a.application_no AS applicationNo,
                    a.user_id AS userId,
                    a.selected_stall_id AS selectedStallId,
                    a.review_status AS reviewStatus,
                    a.payment_status AS paymentStatus,
                    a.deposit_status AS depositStatus,
                    a.is_cancelled AS isCancelled,
                    selected_stall.stall_no AS selectedStallNo,
                    selected_stall.width AS selectedStallWidth,
                    selected_stall.length AS selectedStallLength,
                    selected_stall.height AS selectedStallHeight,
                    selected_zone.zone_name AS selectedStallZoneName,
                    up.name AS vendorName,
                    e.id AS eventId,
                    e.title AS eventTitle,
                    e.city,
                    e.district,
                    e.address,
                    e.start_at AS startAt,
                    e.end_at AS endAt,
                    e.end_at AS eventEndAt,
                    refund_data.refundStatus
                FROM dbo.event_applications a
                INNER JOIN dbo.vendor_profiles vp ON vp.id = a.vendor_profile_id
                INNER JOIN dbo.user_profiles up ON up.id = vp.user_profile_id
                INNER JOIN dbo.market_events e ON e.id = a.event_id
                LEFT JOIN dbo.event_stalls selected_stall ON selected_stall.id = a.selected_stall_id
                LEFT JOIN dbo.event_stall_zones selected_zone ON selected_zone.id = selected_stall.zone_id
                OUTER APPLY (
                    SELECT TOP 1 r.refund_status AS refundStatus
                    FROM dbo.refunds r
                    WHERE r.application_id = a.id
                    ORDER BY
                        CASE r.refund_status
                            WHEN N'REFUNDED' THEN 1
                            WHEN N'REFUNDING' THEN 2
                            WHEN N'REFUND_REQUESTED' THEN 3
                            WHEN N'REFUND_FAILED' THEN 4
                            ELSE 5
                        END,
                        r.id DESC
                ) refund_data
                WHERE a.application_no = :applicationNo
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("applicationNo", applicationNo);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public List<Map<String, Object>> findEventStallsMap(Long eventId) {
        String sql = """
                SELECT
                    s.id AS stallId,
                    s.zone_id AS zoneId,
                    z.zone_name AS zoneName,
                    s.stall_no AS stallNo,
                    s.width,
                    s.length,
                    s.height,
                    s.status
                FROM dbo.event_stalls s
                INNER JOIN dbo.event_stall_zones z ON z.id = s.zone_id
                WHERE s.event_id = :eventId
                ORDER BY z.zone_name ASC, s.stall_no ASC
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        return RepositoryResultMapper.normalizeList(namedParameterJdbcTemplate.queryForList(sql, map));
    }

    public Optional<Map<String, Object>> findOrganizerStallMapEvent(Long organizerUserId, Long eventId) {
        String sql = """
                SELECT
                    e.id AS eventId,
                    e.title AS eventTitle,
                    e.city,
                    e.district,
                    e.address,
                    e.start_at AS startAt,
                    e.end_at AS endAt,
                    e.map_image_url AS mapImageUrl
                FROM dbo.market_events e
                WHERE e.id = :eventId
                  AND e.user_id = :organizerUserId
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("organizerUserId", organizerUserId);
        map.put("eventId", eventId);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public Optional<Map<String, Object>> findOrganizerStallMapDetail(
            Long organizerUserId,
            Long eventId,
            String stallNo) {
        String sql = """
                SELECT
                    s.id AS stallId,
                    s.event_id AS eventId,
                    s.zone_id AS zoneId,
                    z.zone_name AS zoneName,
                    s.stall_no AS stallNo,
                    s.width,
                    s.length,
                    s.height,
                    s.status AS stallStatus,
                    a.id AS applicationId,
                    a.application_no AS applicationNo,
                    a.review_status AS reviewStatus,
                    a.payment_status AS paymentStatus,
                    a.deposit_status AS depositStatus,
                    a.is_cancelled AS isCancelled,
                    a.total_amount AS totalAmount,
                    a.created_at AS appliedAt,
                    selected_at.selectedAt,
                    up.name AS brandName,
                    up.contact_name AS vendorOwnerName,
                    up.contact_phone AS vendorPhone,
                    up.contact_email AS vendorEmail,
                    vp.brand_description AS brandDescription,
                    c.name AS categoryName,
                    refund_data.refundStatus
                FROM dbo.event_stalls s
                INNER JOIN dbo.event_stall_zones z ON z.id = s.zone_id
                INNER JOIN dbo.market_events e ON e.id = s.event_id
                LEFT JOIN dbo.event_applications a ON a.selected_stall_id = s.id
                LEFT JOIN dbo.vendor_profiles vp ON vp.id = a.vendor_profile_id
                LEFT JOIN dbo.user_profiles up ON up.id = vp.user_profile_id
                LEFT JOIN dbo.categories c ON c.id = vp.category_id
                OUTER APPLY (
                    SELECT TOP 1 r.refund_status AS refundStatus
                    FROM dbo.refunds r
                    WHERE r.application_id = a.id
                    ORDER BY
                        CASE r.refund_status
                            WHEN N'REFUNDED' THEN 1
                            WHEN N'REFUNDING' THEN 2
                            WHEN N'REFUND_REQUESTED' THEN 3
                            WHEN N'REFUND_FAILED' THEN 4
                            ELSE 5
                        END,
                        r.id DESC
                ) refund_data
                OUTER APPLY (
                    SELECT TOP 1 rl.created_at AS selectedAt
                    FROM dbo.status_logs sl
                    INNER JOIN dbo.request_logs rl ON rl.id = sl.request_log_id
                    WHERE rl.status_code BETWEEN 200 AND 299
                      AND (
                          (sl.target_type = N'EVENT_APPLICATION'
                              AND sl.target_id = a.id
                              AND sl.status_field = N'event_applications.selected_stall_id'
                              AND sl.new_status = CONVERT(NVARCHAR(100), s.id))
                          OR
                          (sl.target_type = N'EVENT_STALL'
                              AND sl.target_id = s.id
                              AND sl.status_field = N'event_stalls.status'
                              AND sl.new_status = N'SELECTED')
                      )
                    ORDER BY rl.created_at DESC, sl.id DESC
                ) selected_at
                WHERE s.event_id = :eventId
                  AND s.stall_no = :stallNo
                  AND e.user_id = :organizerUserId
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("organizerUserId", organizerUserId);
        map.put("eventId", eventId);
        map.put("stallNo", stallNo);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public List<Map<String, Object>> findEventStallsStatus(Long eventId) {
        String sql = """
                SELECT
                    s.event_id AS eventId,
                    s.zone_id AS zoneId,
                    z.zone_name AS zoneName,
                    s.stall_no AS stallNo,
                    s.status,
                    up.name AS vendorName
                FROM dbo.event_stalls s
                INNER JOIN dbo.event_stall_zones z ON z.id = s.zone_id
                LEFT JOIN dbo.event_applications a ON a.selected_stall_id = s.id
                LEFT JOIN dbo.vendor_profiles vp ON vp.id = a.vendor_profile_id
                LEFT JOIN dbo.user_profiles up ON up.id = vp.user_profile_id
                WHERE s.event_id = :eventId
                ORDER BY z.zone_name ASC, s.stall_no ASC
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        return RepositoryResultMapper.normalizeList(namedParameterJdbcTemplate.queryForList(sql, map));
    }
}
