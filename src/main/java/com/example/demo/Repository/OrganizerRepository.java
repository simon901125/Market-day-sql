package com.example.demo.Repository;

import java.time.LocalDateTime;
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

    public List<Map<String, Object>> findOrganizerApplications(
            Long organizerUserId,
            String eventTitle,
            String brandName,
            LocalDateTime appliedStartAt,
            LocalDateTime appliedEndExclusive) {
        String sql = """
                SELECT
                    a.id AS applicationId,
                    a.application_no AS applicationNo,
                    e.id AS eventId,
                    e.title AS eventTitle,
                    CONCAT(
                        CONVERT(varchar(16), e.start_at, 120),
                        N' - ',
                        CONVERT(varchar(16), e.end_at, 120)
                    ) AS eventTime,
                    e.start_at AS eventStartAt,
                    e.end_at AS eventEndAt,
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
                  AND (:eventTitle IS NULL OR e.title LIKE N'%' + :eventTitle + N'%')
                  AND (:brandName IS NULL OR vendor_up.name LIKE N'%' + :brandName + N'%')
                  AND (:appliedStartAt IS NULL OR a.created_at >= :appliedStartAt)
                  AND (:appliedEndExclusive IS NULL OR a.created_at < :appliedEndExclusive)
                ORDER BY
                    a.created_at DESC,
                    a.id DESC
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("organizerUserId", organizerUserId);
        map.put("eventTitle", normalizeText(eventTitle));
        map.put("brandName", normalizeText(brandName));
        map.put("appliedStartAt", appliedStartAt);
        map.put("appliedEndExclusive", appliedEndExclusive);

        return RepositoryResultMapper.normalizeList(namedParameterJdbcTemplate.queryForList(sql, map));
    }

    public Optional<Map<String, Object>> findOrganizerApplicationDetail(Long organizerUserId, Long applicationId) {
        String sql = """
                SELECT
                    a.id AS applicationId,
                    a.application_no AS applicationNo,
                    a.event_id AS eventId,
                    e.title AS eventTitle,
                    e.summary AS eventSummary,
                    e.description AS eventDescription,
                    e.location_name AS locationName,
                    e.city AS eventCity,
                    e.district AS eventDistrict,
                    e.address AS eventAddress,
                    e.start_at AS eventStartAt,
                    e.end_at AS eventEndAt,
                    e.base_fee AS baseFee,
                    e.cover_image_url AS eventCoverImageUrl,
                    vendor_user.id AS vendorUserId,
                    vendor_user.email AS vendorEmail,
                    vendor_up.id AS vendorUserProfileId,
                    vendor_up.name AS vendorName,
                    vendor_up.contact_name AS vendorOwnerName,
                    vendor_up.contact_phone AS vendorPhone,
                    vendor_up.contact_email AS vendorContactEmail,
                    vendor_up.city AS vendorCity,
                    vendor_up.district AS vendorDistrict,
                    vendor_up.address AS vendorAddress,
                    vp.id AS vendorProfileId,
                    vp.brand_type AS brandType,
                    vp.brand_description AS brandDescription,
                    vp.product_summary AS productSummary,
                    vp.instagram_url AS instagramUrl,
                    vp.facebook_url AS facebookUrl,
                    vp.website_url AS websiteUrl,
                    c.name AS categoryName,
                    vendor_avatar.image_url AS vendorAvatarUrl,
                    a.selected_stall_id AS selectedStallId,
                    selected_stall.stall_no AS selectedStallNo,
                    selected_stall.width AS stallWidth,
                    selected_stall.length AS stallLength,
                    selected_stall.height AS stallHeight,
                    selected_zone.zone_name AS stallZoneName,
                    a.vehicle_no AS vehicleNo,
                    a.applicant_note AS applicantNote,
                    a.total_amount AS totalAmount,
                    a.deposit_amount AS depositAmount,
                    a.deposit_status AS depositStatus,
                    a.payment_due_at AS paymentDueAt,
                    a.review_status AS reviewStatus,
                    a.review_note AS reviewNote,
                    a.payment_status AS paymentStatus,
                    a.is_cancelled AS isCancelled,
                    a.created_at AS appliedAt,
                    application_dates.applyDates,
                    latest_payment.paymentNo,
                    latest_payment.paymentAmount,
                    latest_payment.paymentProvider,
                    latest_payment.paymentProviderTradeNo,
                    latest_payment.paymentRecordStatus,
                    latest_payment.paidAt,
                    latest_payment.paymentCreatedAt,
                    latest_refund.refundNo,
                    latest_refund.refundAmount,
                    latest_refund.refundStatus,
                    latest_refund.refundedAt
                FROM dbo.event_applications a
                INNER JOIN dbo.market_events e ON e.id = a.event_id
                INNER JOIN dbo.users vendor_user ON vendor_user.id = a.user_id
                INNER JOIN dbo.vendor_profiles vp ON vp.id = a.vendor_profile_id
                INNER JOIN dbo.user_profiles vendor_up ON vendor_up.id = vp.user_profile_id
                INNER JOIN dbo.categories c ON c.id = vp.category_id
                LEFT JOIN dbo.event_stalls selected_stall ON selected_stall.id = a.selected_stall_id
                LEFT JOIN dbo.event_stall_zones selected_zone ON selected_zone.id = selected_stall.zone_id
                OUTER APPLY (
                    SELECT TOP 1 vi.image_url
                    FROM dbo.vendor_images vi
                    WHERE vi.vendor_profile_id = vp.id
                      AND vi.image_type = N'AVATAR'
                    ORDER BY vi.id DESC
                ) vendor_avatar
                OUTER APPLY (
                    SELECT STRING_AGG(CONVERT(varchar(10), ad.apply_date, 23), ',') WITHIN GROUP (ORDER BY ad.apply_date) AS applyDates
                    FROM dbo.application_dates ad
                    WHERE ad.application_id = a.id
                ) application_dates
                OUTER APPLY (
                    SELECT TOP 1
                        p.payment_no AS paymentNo,
                        p.amount AS paymentAmount,
                        p.provider AS paymentProvider,
                        p.provider_trade_no AS paymentProviderTradeNo,
                        p.status AS paymentRecordStatus,
                        p.paid_at AS paidAt,
                        p.created_at AS paymentCreatedAt
                    FROM dbo.payments p
                    WHERE p.application_id = a.id
                    ORDER BY p.created_at DESC, p.id DESC
                ) latest_payment
                OUTER APPLY (
                    SELECT TOP 1
                        r.refund_no AS refundNo,
                        r.amount AS refundAmount,
                        r.refund_status AS refundStatus,
                        r.refunded_at AS refundedAt
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
                ) latest_refund
                WHERE a.id = :applicationId
                  AND e.user_id = :organizerUserId
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("organizerUserId", organizerUserId);
        map.put("applicationId", applicationId);

        return RepositoryResultMapper.normalizeOptional(namedParameterJdbcTemplate.queryForList(sql, map).stream().findFirst());
    }

    public List<Map<String, Object>> findApplicationStatusLogs(Long applicationId) {
        String sql = """
                SELECT
                    sl.status_field AS statusField,
                    sl.new_status AS newStatus,
                    rl.created_at AS createdAt
                FROM dbo.status_logs sl
                INNER JOIN dbo.request_logs rl ON rl.id = sl.request_log_id
                WHERE sl.target_type = N'EVENT_APPLICATION'
                  AND sl.target_id = :applicationId
                ORDER BY rl.created_at ASC, sl.id ASC
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("applicationId", applicationId);
        return RepositoryResultMapper.normalizeList(namedParameterJdbcTemplate.queryForList(sql, map));
    }

    public List<Map<String, Object>> findApplicationEquipmentRentals(Long applicationId) {
        String sql = """
                SELECT
                    er.id AS equipmentRentalId,
                    er.event_equipment_id AS eventEquipmentId,
                    er.equipment_name AS equipmentName,
                    er.rental_fee AS rentalFee,
                    er.pricing_unit AS pricingUnit,
                    er.quantity,
                    er.rental_units AS rentalUnits,
                    er.subtotal,
                    ra.id AS applianceId,
                    ra.appliance_name AS applianceName,
                    ra.wattage
                FROM dbo.equipment_rentals er
                LEFT JOIN dbo.rental_appliances ra ON ra.equipment_rental_id = er.id
                WHERE er.application_id = :applicationId
                ORDER BY er.id ASC, ra.id ASC
                """;

        Map<String, Object> map = new HashMap<>();
        map.put("applicationId", applicationId);
        return RepositoryResultMapper.normalizeList(namedParameterJdbcTemplate.queryForList(sql, map));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
