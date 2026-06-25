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

    public Optional<Long> findAvailableStallId(Long eventId, String stallNo) {
        String sql = """
                SELECT id
                FROM dbo.event_stalls
                WHERE event_id = :eventId
                  AND stall_no = :stallNo
                  AND status = N'AVAILABLE'
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("stallNo", stallNo);

        List<Long> stallIds = namedParameterJdbcTemplate.queryForList(sql, map, Long.class);
        return stallIds.stream().findFirst();
    }

    public Optional<Map<String, Object>> findSelectableApplication(Long eventId, String applicationNo) {
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
                WHERE event_id = :eventId
                  AND application_no = :applicationNo
                  AND review_status = N'APPROVED'
                  AND payment_status = N'PAID'
                  AND selected_stall_id IS NULL
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
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

    public Optional<Map<String, Object>> findVendorStallMapApplication(String applicationNo) {
        String sql = """
                SELECT
                    a.application_no AS applicationNo,
                    a.selected_stall_id AS selectedStallId,
                    up.name AS vendorName,
                    e.id AS eventId,
                    e.title AS eventTitle,
                    e.address,
                    e.start_date AS startDate,
                    e.end_date AS endDate
                FROM dbo.event_applications a
                INNER JOIN dbo.vendor_profiles vp ON vp.id = a.vendor_profile_id
                INNER JOIN dbo.user_profiles up ON up.id = vp.user_profile_id
                INNER JOIN dbo.market_events e ON e.id = a.event_id
                WHERE a.application_no = :applicationNo
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("applicationNo", applicationNo);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public List<Map<String, Object>> findEventStallsMap(Long eventId) {
        String sql = """
                SELECT
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
