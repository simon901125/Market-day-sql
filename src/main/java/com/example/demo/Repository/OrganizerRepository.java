package com.example.demo.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrganizerRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<Map<String, Object>> findOrganizerAccountByEmail(String email) {
        String sql = """
                SELECT
                    u.id AS userId,
                    u.role,
                    up.name AS organizerName,
                    up.contact_name AS contactName,
                    up.contact_phone AS contactPhone,
                    up.contact_email AS contactEmail,
                    op.company_name AS companyName,
                    op.tax_id AS taxId,
                    up.city,
                    up.district,
                    up.address,
                    op.service_days AS serviceDays,
                    op.service_start_time AS serviceStartTime,
                    op.service_end_time AS serviceEndTime
                FROM dbo.users u
                INNER JOIN dbo.user_profiles up ON up.user_id = u.id
                    AND up.profile_type = N'ORGANIZER'
                INNER JOIN dbo.organizer_profiles op ON op.user_profile_id = up.id
                WHERE u.email = :email
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("email", email);

        List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sql, map);
        return RepositoryResultMapper.normalizeOptional(list.stream().findFirst());
    }

    public List<Map<String, Object>> findOrganizerApplications(Long organizerUserId) {
        String sql = """
                SELECT
                    a.id AS applicationId,
                    a.application_no AS applicationNo,
                    e.id AS eventId,
                    e.title AS eventTitle,
                    CONCAT(
                        CONVERT(varchar(10), e.start_date, 23),
                        CASE WHEN e.start_time IS NULL THEN N'' ELSE N' ' + CONVERT(varchar(5), e.start_time, 108) END,
                        N' - ',
                        CONVERT(varchar(10), e.end_date, 23),
                        CASE WHEN e.end_time IS NULL THEN N'' ELSE N' ' + CONVERT(varchar(5), e.end_time, 108) END
                    ) AS eventTime,
                    e.start_date AS eventStartDate,
                    e.end_date AS eventEndDate,
                    e.start_time AS eventStartTime,
                    e.end_time AS eventEndTime,
                    vendor_up.name AS vendorName,
                    vendor_user.name AS vendorOwnerName,
                    vp.brand_type AS brandType,
                    a.created_at AS appliedAt,
                    application_dates.applyDates,
                    a.review_status AS reviewStatus,
                    a.payment_status AS paymentStatus,
                    a.deposit_status AS depositStatus,
                    a.selected_stall_id AS selectedStallId,
                    a.is_cancelled AS isCancelled,
                    refund_data.refundStatus
                FROM dbo.event_applications a
                INNER JOIN dbo.market_events e ON e.id = a.event_id
                INNER JOIN dbo.users vendor_user ON vendor_user.id = a.user_id
                INNER JOIN dbo.vendor_profiles vp ON vp.id = a.vendor_profile_id
                INNER JOIN dbo.user_profiles vendor_up ON vendor_up.id = vp.user_profile_id
                OUTER APPLY (
                    SELECT STRING_AGG(CONVERT(varchar(10), ad.apply_date, 23), ',') WITHIN GROUP (ORDER BY ad.apply_date) AS applyDates
                    FROM dbo.application_dates ad
                    WHERE ad.application_id = a.id
                ) application_dates
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
                WHERE e.user_id = :organizerUserId
                  AND e.publish_status = N'PUBLISHED'
                ORDER BY
                    a.created_at DESC,
                    a.id DESC
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("organizerUserId", organizerUserId);

        return RepositoryResultMapper.normalizeList(namedParameterJdbcTemplate.queryForList(sql, map));
    }
}
