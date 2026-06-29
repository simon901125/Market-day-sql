package com.example.demo.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Repository.OrganizerRepository;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.OrganizerAccountResponse;
import com.example.demo.dto.response.OrganizerApplicationDetailResponse;
import com.example.demo.dto.response.OrganizerApplicationSearchResponse;
import com.example.demo.dto.response.OrganizerApplicationSummaryResponse;

@Service
public class OrganizerService {

    private static final DateTimeFormatter SERVICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SPACE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationStatusService applicationStatusService;

    @Autowired
    private PaymentStatusService paymentStatusService;

    public ApiResponse<OrganizerAccountResponse> getOrganizerAccount(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return ApiResponse.fail("Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return ApiResponse.fail("Invalid or expired token");
        }

        String email = jwtService.getEmail(token);
        Map<String, Object> organizer = organizerRepository.findOrganizerAccountByEmail(email)
                .orElse(null);
        if (organizer == null) {
            return ApiResponse.fail("Organizer profile not found");
        }
        if (!"ORGANIZER".equals(organizer.get("role"))) {
            return ApiResponse.fail("This account is not an organizer");
        }

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("organizerName", organizer.get("organizerName"));
        account.put("contactName", organizer.get("contactName"));
        account.put("contactPhone", organizer.get("contactPhone"));
        account.put("contactEmail", organizer.get("contactEmail"));
        account.put("companyName", organizer.get("companyName"));
        account.put("taxId", organizer.get("taxId"));
        account.put("city", organizer.get("city"));
        account.put("district", organizer.get("district"));
        account.put("address", joinAddress(
                organizer.get("city"),
                organizer.get("district"),
                organizer.get("address")));
        account.put("serviceDays", organizer.get("serviceDays"));
        account.put("serviceTime", formatServiceTime(
                organizer.get("serviceStartTime"),
                organizer.get("serviceEndTime")));
        return ApiResponse.success("Organizer account retrieved successfully", new OrganizerAccountResponse(account));
    }

    public ApiResponse<OrganizerApplicationSearchResponse> searchOrganizerApplications(
            String authorizationHeader,
            String eventTitle,
            String status,
            String brandName,
            LocalDate registrationStartAt,
            LocalDate registrationEndAt) {
        Map<String, Object> organizer = getAuthenticatedOrganizer(authorizationHeader);
        if (organizer.containsKey("message")) {
            return ApiResponse.fail(organizer.get("message").toString());
        }

        Long organizerUserId = ((Number) organizer.get("userId")).longValue();
        LocalDateTime appliedStartAt = registrationStartAt == null ? null : registrationStartAt.atStartOfDay();
        LocalDateTime appliedEndExclusive = registrationEndAt == null ? null : registrationEndAt.plusDays(1).atStartOfDay();
        List<OrganizerApplicationSummaryResponse> applications = organizerRepository
                .findOrganizerApplications(organizerUserId, eventTitle, brandName, appliedStartAt, appliedEndExclusive)
                .stream()
                .map(this::withDisplayApplicationStatus)
                .filter(application -> matchesApplicationStatus(application, status))
                .map(this::toApplicationSummaryResponse)
                .map(OrganizerApplicationSummaryResponse::new)
                .toList();
        return ApiResponse.success(
                "Organizer applications retrieved successfully",
                new OrganizerApplicationSearchResponse(applications));
    }

    public ApiResponse<OrganizerApplicationDetailResponse> getOrganizerApplicationDetail(String authorizationHeader, Long applicationId) {
        Map<String, Object> organizer = getAuthenticatedOrganizer(authorizationHeader);
        if (organizer.containsKey("message")) {
            return ApiResponse.fail(organizer.get("message").toString());
        }
        if (applicationId == null) {
            return ApiResponse.fail("Application id is required");
        }

        Long organizerUserId = ((Number) organizer.get("userId")).longValue();
        Map<String, Object> application = organizerRepository
                .findOrganizerApplicationDetail(organizerUserId, applicationId)
                .orElse(null);
        if (application == null) {
            return ApiResponse.fail("Application not found");
        }

        return ApiResponse.success(
                "Organizer application detail retrieved successfully",
                new OrganizerApplicationDetailResponse(toApplicationDetailResponse(withDisplayApplicationStatus(application))));
    }

    private Map<String, Object> withDisplayApplicationStatus(Map<String, Object> application) {
        Map<String, Object> response = new LinkedHashMap<>(application);
        response.put("applicationStatus", applicationStatusService.resolveApplicationStatus(application));
        return response;
    }

    private boolean matchesApplicationStatus(Map<String, Object> application, String status) {
        String normalizedStatus = normalizeText(status);
        if (normalizedStatus == null || "全部".equals(normalizedStatus)) {
            return true;
        }
        return normalizedStatus.equals(normalizeText(application.get("applicationStatus")));
    }

    private Map<String, Object> toApplicationSummaryResponse(Map<String, Object> application) {
        return orderedMap(
                "applicationId", application.get("applicationId"),
                "eventTitle", application.get("eventTitle"),
                "eventTime", application.get("eventTime"),
                "vendorName", application.get("vendorName"),
                "brandType", application.get("brandType"),
                "vendorOwnerName", application.get("vendorOwnerName"),
                "appliedAt", formatAppliedAt(application),
                "applicationStatus", application.get("applicationStatus"));
    }

    private Map<String, Object> toApplicationDetailResponse(Map<String, Object> application) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("applicationId", application.get("applicationId"));
        response.put("applicationNo", application.get("applicationNo"));
        response.put("applicationStatus", application.get("applicationStatus"));

        response.put("event", orderedMap(
                "eventId", application.get("eventId"),
                "eventTitle", application.get("eventTitle"),
                "eventSummary", application.get("eventSummary"),
                "eventDescription", application.get("eventDescription"),
                "eventTime", formatEventTime(application),
                "eventStartAt", application.get("eventStartAt"),
                "eventEndAt", application.get("eventEndAt"),
                "locationName", application.get("locationName"),
                "city", application.get("eventCity"),
                "district", application.get("eventDistrict"),
                "address", joinAddress(
                        application.get("eventCity"),
                        application.get("eventDistrict"),
                        application.get("eventAddress")),
                "coverImageUrl", application.get("eventCoverImageUrl")));

        response.put("application", orderedMap(
                "applicationNo", application.get("applicationNo"),
                "applicationStatus", application.get("applicationStatus"),
                "reviewStatus", application.get("reviewStatus"),
                "paymentStatus", application.get("paymentStatus"),
                "paymentDisplayStatus", paymentStatusService.resolvePaymentStatus(application),
                "depositStatus", application.get("depositStatus"),
                "refundStatus", application.get("refundStatus"),
                "isCancelled", application.get("isCancelled"),
                "appliedAt", application.get("appliedAt"),
                "paymentDueAt", application.get("paymentDueAt"),
                "reviewNote", application.get("reviewNote"),
                "applicantNote", application.get("applicantNote"),
                "vehicleNo", application.get("vehicleNo")));

        response.put("statusTimeline", orderedMap(
                "appliedAt", application.get("appliedAt"),
                "reviewedAt", null,
                "paymentCreatedAt", application.get("paymentCreatedAt"),
                "paidAt", application.get("paidAt"),
                "refundRequestedAt", null,
                "refundReviewedAt", null,
                "refundedAt", application.get("refundedAt")));

        response.put("vendor", orderedMap(
                "vendorUserId", application.get("vendorUserId"),
                "vendorProfileId", application.get("vendorProfileId"),
                "vendorName", application.get("vendorName"),
                "vendorOwnerName", application.get("vendorOwnerName"),
                "vendorPhone", application.get("vendorPhone"),
                "vendorEmail", application.get("vendorContactEmail"),
                "loginEmail", application.get("vendorEmail"),
                "city", application.get("vendorCity"),
                "district", application.get("vendorDistrict"),
                "address", joinAddress(
                        application.get("vendorCity"),
                        application.get("vendorDistrict"),
                        application.get("vendorAddress"))));

        response.put("brand", orderedMap(
                "brandName", application.get("vendorName"),
                "brandType", application.get("brandType"),
                "categoryName", application.get("categoryName"),
                "brandDescription", application.get("brandDescription"),
                "productSummary", application.get("productSummary"),
                "avatarUrl", application.get("vendorAvatarUrl"),
                "instagramUrl", application.get("instagramUrl"),
                "facebookUrl", application.get("facebookUrl"),
                "websiteUrl", application.get("websiteUrl")));

        response.put("registration", orderedMap(
                "applyDates", application.get("applyDates"),
                "stall", orderedMap(
                        "selectedStallId", application.get("selectedStallId"),
                        "stallNo", application.get("selectedStallNo"),
                        "zoneName", application.get("stallZoneName"),
                        "width", application.get("stallWidth"),
                        "length", application.get("stallLength"),
                        "height", application.get("stallHeight"))));

        Object baseFee = application.get("baseFee");
        Object depositAmount = application.get("depositAmount");
        Object totalAmount = application.get("totalAmount");
        response.put("fee", orderedMap(
                "baseFee", baseFee,
                "depositAmount", depositAmount,
                "otherFeeAmount", subtractAmounts(totalAmount, baseFee, depositAmount),
                "totalAmount", totalAmount,
                "payment", orderedMap(
                        "paymentNo", application.get("paymentNo"),
                        "paymentAmount", application.get("paymentAmount"),
                        "paymentProvider", application.get("paymentProvider"),
                        "providerTradeNo", application.get("paymentProviderTradeNo"),
                        "paymentStatus", application.get("paymentRecordStatus"),
                        "paidAt", application.get("paidAt")),
                "refund", orderedMap(
                        "refundNo", application.get("refundNo"),
                        "refundAmount", application.get("refundAmount"),
                        "refundStatus", application.get("refundStatus"),
                        "refundedAt", application.get("refundedAt"))));

        return response;
    }

    private Map<String, Object> getAuthenticatedOrganizer(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }

        String email = jwtService.getEmail(token);
        Map<String, Object> organizer = organizerRepository.findOrganizerAccountByEmail(email)
                .orElse(null);
        if (organizer == null) {
            return Map.of("message", "Organizer profile not found");
        }
        if (!"ORGANIZER".equals(organizer.get("role"))) {
            return Map.of("message", "This account is not an organizer");
        }
        return organizer;
    }

    private String formatServiceTime(Object startTime, Object endTime) {
        if (!(startTime instanceof LocalTime start) || !(endTime instanceof LocalTime end)) {
            return null;
        }
        return start.format(SERVICE_TIME_FORMATTER) + "-" + end.format(SERVICE_TIME_FORMATTER);
    }

    private String formatEventTime(Map<String, Object> application) {
        LocalDateTime startAt = toLocalDateTime(application.get("eventStartAt"));
        LocalDateTime endAt = toLocalDateTime(application.get("eventEndAt"));
        if (startAt == null || endAt == null) {
            return null;
        }
        return startAt.format(DISPLAY_DATE_TIME_FORMATTER) + " - " + endAt.format(DISPLAY_DATE_TIME_FORMATTER);
    }

    private String formatAppliedAt(Map<String, Object> application) {
        LocalDateTime appliedAt = appliedAtForSort(application);
        return appliedAt == null ? null : appliedAt.format(DISPLAY_DATE_TIME_FORMATTER);
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String joinAddress(Object city, Object district, Object address) {
        StringBuilder builder = new StringBuilder();
        appendIfPresent(builder, city);
        appendIfPresent(builder, district);
        appendIfPresent(builder, address);
        return builder.isEmpty() ? null : builder.toString();
    }

    private void appendIfPresent(StringBuilder builder, Object value) {
        String text = normalizeText(value);
        if (text != null) {
            builder.append(text);
        }
    }

    private Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return map;
    }

    private BigDecimal subtractAmounts(Object totalAmount, Object baseFee, Object depositAmount) {
        BigDecimal total = toBigDecimal(totalAmount);
        if (total == null) {
            return null;
        }
        BigDecimal base = toBigDecimal(baseFee);
        BigDecimal deposit = toBigDecimal(depositAmount);
        return total
                .subtract(base == null ? BigDecimal.ZERO : base)
                .subtract(deposit == null ? BigDecimal.ZERO : deposit);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = normalizeText(value);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private LocalDateTime appliedAtForSort(Map<String, Object> application) {
        Object value = application.get("appliedAt");
        return toLocalDateTime(value);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        String text = normalizeText(value);
        if (text == null) {
            return null;
        }
        try {
            if (text.contains(" ")) {
                return LocalDateTime.parse(text, SPACE_DATE_TIME_FORMATTER);
            }
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
