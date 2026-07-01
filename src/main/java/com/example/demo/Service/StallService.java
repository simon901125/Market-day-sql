package com.example.demo.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.example.demo.Repository.OrganizerRepository;
import com.example.demo.Repository.StallRepository;
import com.example.demo.dto.request.StallSelectionRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.EventStallStatusResponse;
import com.example.demo.dto.response.MapBackedResponse;
import com.example.demo.dto.response.StallSelectionResponse;
import com.example.demo.dto.response.VendorAccountResponse;
import com.example.demo.dto.response.VendorStallMapResponse;

@Service
public class StallService {

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationStatusService applicationStatusService;

    @Transactional
    public ApiResponse<StallSelectionResponse> selectEventStall(
            String authorizationHeader,
            StallSelectionRequest body) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return ApiResponse.fail("Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return ApiResponse.fail("Invalid or expired token");
        }
        if (!"VENDOR".equals(jwtService.getRole(token))) {
            return ApiResponse.fail("This account is not a vendor");
        }

        if (body.getApplicationNo() == null || body.getApplicationNo().isBlank()) {
            return ApiResponse.fail("Application number is required");
        }

        if (body.getStallNo() == null || body.getStallNo().isBlank()) {
            return ApiResponse.fail("Stall number is required");
        }

        String email = jwtService.getEmail(token);
        Map<String, Object> vendor = stallRepository.findVendorAccountByEmail(email)
                .orElse(null);
        if (vendor == null) {
            return ApiResponse.fail("Vendor profile not found");
        }
        if (!"VENDOR".equals(vendor.get("role"))) {
            return ApiResponse.fail("This account is not a vendor");
        }

        Map<String, Object> application = stallRepository.findApplicationForSelection(body.getApplicationNo())
                .orElse(null);
        if (application == null) {
            return ApiResponse.fail("Application not found");
        }

        Long vendorUserId = ((Number) vendor.get("userId")).longValue();
        Long applicationUserId = ((Number) application.get("userId")).longValue();
        if (!vendorUserId.equals(applicationUserId)) {
            return ApiResponse.fail("Application does not belong to this account");
        }

        if (isTrue(application.get("isCancelled"))) {
            return ApiResponse.fail("Application has been cancelled");
        }

        String reviewStatus = stringValue(application.get("reviewStatus"));
        if ("PENDING".equals(reviewStatus)) {
            return ApiResponse.fail("Application review is pending");
        }
        if ("REJECTED".equals(reviewStatus)) {
            return ApiResponse.fail("Application review was rejected");
        }
        if (!"APPROVED".equals(reviewStatus)) {
            return ApiResponse.fail("Application is not approved");
        }

        String paymentStatus = stringValue(application.get("paymentStatus"));
        if ("PENDING".equals(paymentStatus)) {
            return ApiResponse.fail("Application payment is pending");
        }
        if (!"PAID".equals(paymentStatus)) {
            return ApiResponse.fail("Application payment is not paid");
        }

        if (application.get("selectedStallId") != null) {
            return ApiResponse.fail("Application has already selected a stall");
        }

        Long eventId = ((Number) application.get("eventId")).longValue();
        Map<String, Object> stall = stallRepository.findStallForSelection(eventId, body.getStallNo())
                .orElse(null);
        if (stall == null) {
            return ApiResponse.fail("Stall not found");
        }

        String stallStatus = stringValue(stall.get("status"));
        if ("SELECTED".equals(stallStatus)) {
            return ApiResponse.fail("Stall has already been selected");
        }
        if (!"AVAILABLE".equals(stallStatus)) {
            return ApiResponse.fail("Stall is not available");
        }

        Long stallId = ((Number) stall.get("id")).longValue();
        // Claim the stall first so concurrent selection has one winner.
        int stallUpdatedRows = stallRepository.selectAvailableStall(stallId);
        if (stallUpdatedRows == 0) {
            return ApiResponse.fail("Stall has already been selected");
        }

        int applicationUpdatedRows = stallRepository.bindApplicationSelectedStall(
                eventId,
                body.getApplicationNo(),
                stallId);
        if (applicationUpdatedRows == 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.fail("Application status changed during stall selection");
        }

        return ApiResponse.success(
                "Stall selection successful",
                new StallSelectionResponse(body.getApplicationNo(), body.getStallNo()));
    }

    public ApiResponse<List<EventStallStatusResponse>> getEventStallsStatus(Long eventId) {
        List<EventStallStatusResponse> stalls = stallRepository.findEventStallsStatus(eventId).stream()
                .map(this::withDisplayBoothStatus)
                .map(EventStallStatusResponse::new)
                .toList();
        return ApiResponse.success("Event stalls status retrieved successfully", stalls);
    }

    public ApiResponse<VendorAccountResponse> getVendorAccount(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return ApiResponse.fail("Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return ApiResponse.fail("Invalid or expired token");
        }

        String email = jwtService.getEmail(token);
        Map<String, Object> vendor = stallRepository.findVendorAccountByEmail(email)
                .orElse(null);
        if (vendor == null) {
            return ApiResponse.fail("Vendor profile not found");
        }
        if (!"VENDOR".equals(vendor.get("role"))) {
            return ApiResponse.fail("This account is not a vendor");
        }

        String provider = vendor.get("provider") == null ? "" : vendor.get("provider").toString().toLowerCase();
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("userId", vendor.get("userId"));
        account.put("userProfileId", vendor.get("userProfileId"));
        account.put("vendorProfileId", vendor.get("vendorProfileId"));
        account.put("name", vendor.get("name"));
        account.put("email", vendor.get("email"));
        account.put("loginProvider", provider);
        account.put("contactName", vendor.get("contactName"));
        account.put("contactPhone", vendor.get("contactPhone"));
        account.put("contactEmail", vendor.get("contactEmail"));
        account.put("city", vendor.get("city"));
        account.put("district", vendor.get("district"));
        account.put("address", joinAddress(
                vendor.get("city"),
                vendor.get("district"),
                vendor.get("address")));
        account.put("categoryId", vendor.get("categoryId"));
        account.put("categoryName", vendor.get("categoryName"));
        account.put("categorySlug", vendor.get("categorySlug"));
        account.put("instagramUrl", vendor.get("instagramUrl"));
        account.put("facebookUrl", vendor.get("facebookUrl"));
        account.put("websiteUrl", vendor.get("websiteUrl"));
        account.put("brandDescription", vendor.get("brandDescription"));
        account.put("brandType", vendor.get("brandType"));
        account.put("productSummary", vendor.get("productSummary"));
        return ApiResponse.success("Vendor account retrieved successfully", new VendorAccountResponse(account));
    }

    public ApiResponse<VendorStallMapResponse> getVendorStallMap(String authorizationHeader, String applicationNo) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return ApiResponse.fail("Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return ApiResponse.fail("Invalid or expired token");
        }
        if (!"VENDOR".equals(jwtService.getRole(token))) {
            return ApiResponse.fail("This account is not a vendor");
        }

        if (applicationNo == null || applicationNo.isBlank()) {
            return ApiResponse.fail("Application number is required");
        }

        String email = jwtService.getEmail(token);
        Map<String, Object> vendor = stallRepository.findVendorAccountByEmail(email)
                .orElse(null);
        if (vendor == null) {
            return ApiResponse.fail("Vendor profile not found");
        }
        if (!"VENDOR".equals(vendor.get("role"))) {
            return ApiResponse.fail("This account is not a vendor");
        }

        Map<String, Object> applicationData = stallRepository.findVendorStallMapApplication(applicationNo)
                .orElse(null);
        if (applicationData == null) {
            return ApiResponse.fail("Application not found");
        }

        Long vendorUserId = ((Number) vendor.get("userId")).longValue();
        Long applicationUserId = ((Number) applicationData.get("userId")).longValue();
        if (!vendorUserId.equals(applicationUserId)) {
            return ApiResponse.fail("Application does not belong to this account");
        }

        String applicationStatus = applicationStatusService.resolveApplicationStatus(applicationData);
        if (!canViewStallMap(applicationStatus, applicationData)) {
            return ApiResponse.fail("Application is not selectable for stall map");
        }

        Long eventId = ((Number) applicationData.get("eventId")).longValue();
        List<Map<String, Object>> stalls = stallRepository.findEventStallsMap(eventId).stream()
                .map(this::withDisplayBoothStatus)
                .toList();

        Map<String, Object> application = new LinkedHashMap<>();
        application.put("applicationNo", applicationData.get("applicationNo"));
        application.put("applicationStatus", applicationStatus);
        application.put("vendorName", applicationData.get("vendorName"));
        application.put("selectedStallId", applicationData.get("selectedStallId"));
        application.put("selectedStall", selectedStall(applicationData));

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventTitle", applicationData.get("eventTitle"));
        event.put("startAt", applicationData.get("startAt"));
        event.put("endAt", applicationData.get("endAt"));
        event.put("address", joinAddress(
                applicationData.get("city"),
                applicationData.get("district"),
                applicationData.get("address")));

        return ApiResponse.success("Vendor stall map retrieved successfully", new VendorStallMapResponse(application, event, stalls));
    }

    public ApiResponse<MapBackedResponse> getOrganizerStallMap(String authorizationHeader, Long eventId) {
        Map<String, Object> organizer = authenticatedOrganizer(authorizationHeader);
        if (organizer.containsKey("message")) {
            return ApiResponse.fail(organizer.get("message").toString());
        }
        if (eventId == null) {
            return ApiResponse.fail("Event id is required");
        }

        Long organizerUserId = ((Number) organizer.get("userId")).longValue();
        Map<String, Object> eventData = stallRepository.findOrganizerStallMapEvent(organizerUserId, eventId)
                .orElse(null);
        if (eventData == null) {
            return ApiResponse.fail("Event not found");
        }

        List<Map<String, Object>> stallRows = stallRepository.findEventStallsMap(eventId).stream()
                .map(this::withDisplayBoothStatus)
                .toList();

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventData.get("eventId"));
        event.put("eventTitle", eventData.get("eventTitle"));
        event.put("startAt", eventData.get("startAt"));
        event.put("endAt", eventData.get("endAt"));
        event.put("address", joinAddress(
                eventData.get("city"),
                eventData.get("district"),
                eventData.get("address")));
        event.put("mapImageUrl", eventData.get("mapImageUrl"));

        return ApiResponse.success(
                "Organizer stall map retrieved successfully",
                new MapBackedResponse(orderedMap(
                        "event", event,
                        "stalls", groupStallsByZone(stallRows))));
    }

    public ApiResponse<MapBackedResponse> getOrganizerStallMapDetail(
            String authorizationHeader,
            Long eventId,
            String stallNo) {
        Map<String, Object> organizer = authenticatedOrganizer(authorizationHeader);
        if (organizer.containsKey("message")) {
            return ApiResponse.fail(organizer.get("message").toString());
        }
        if (eventId == null) {
            return ApiResponse.fail("Event id is required");
        }
        if (stallNo == null || stallNo.isBlank()) {
            return ApiResponse.fail("Stall number is required");
        }

        Long organizerUserId = ((Number) organizer.get("userId")).longValue();
        Map<String, Object> detail = stallRepository
                .findOrganizerStallMapDetail(organizerUserId, eventId, stallNo)
                .orElse(null);
        if (detail == null) {
            return ApiResponse.fail("Stall not found");
        }

        Map<String, Object> applicationData = applicationData(detail);

        Map<String, Object> stall = new LinkedHashMap<>();
        stall.put("stallId", detail.get("stallId"));
        stall.put("stallNo", detail.get("stallNo"));
        stall.put("zoneId", detail.get("zoneId"));
        stall.put("zoneName", detail.get("zoneName"));
        stall.put("width", detail.get("width"));
        stall.put("length", detail.get("length"));
        stall.put("height", detail.get("height"));
        stall.put("status", displayBoothStatus(detail.get("stallStatus")));
        stall.put("selectedAt", detail.get("selectedAt"));

        Map<String, Object> response = orderedMap(
                "stall", stall,
                "application", applicationData == null ? null : orderedMap(
                        "id", detail.get("applicationId")),
                "vendor", applicationData == null ? null : orderedMap(
                        "brandName", detail.get("brandName"),
                        "vendorOwnerName", detail.get("vendorOwnerName"),
                        "vendorPhone", detail.get("vendorPhone"),
                        "vendorEmail", detail.get("vendorEmail")));

        return ApiResponse.success(
                "Organizer stall detail retrieved successfully",
                new MapBackedResponse(response));
    }

    private boolean canViewStallMap(String applicationStatus, Map<String, Object> applicationData) {
        return "待選位".equals(applicationStatus) || isSelectedStallApplication(applicationData);
    }

    private Map<String, Object> authenticatedOrganizer(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }
        if (!"ORGANIZER".equals(jwtService.getRole(token))) {
            return Map.of("message", "This account is not an organizer");
        }

        Map<String, Object> organizer = organizerRepository.findOrganizerAccountByEmail(jwtService.getEmail(token))
                .orElse(null);
        if (organizer == null) {
            return Map.of("message", "Organizer profile not found");
        }
        if (!"ORGANIZER".equals(organizer.get("role"))) {
            return Map.of("message", "This account is not an organizer");
        }
        return organizer;
    }

    private Map<String, Object> applicationData(Map<String, Object> detail) {
        if (detail.get("applicationId") == null) {
            return null;
        }
        return orderedMap(
                "reviewStatus", detail.get("reviewStatus"),
                "paymentStatus", detail.get("paymentStatus"),
                "depositStatus", detail.get("depositStatus"),
                "isCancelled", detail.get("isCancelled"),
                "selectedStallId", detail.get("stallId"),
                "eventEndAt", null,
                "refundStatus", detail.get("refundStatus"));
    }

    private String paymentStatusText(Object status) {
        return switch (stringValue(status)) {
            case "PAID" -> "付款完成";
            case "PENDING" -> "待付款";
            case "FAILED" -> "付款失敗";
            case "EXPIRED" -> "付款逾期";
            default -> normalizeText(status);
        };
    }

    private Map<String, Object> withDisplayBoothStatus(Map<String, Object> stall) {
        Map<String, Object> response = new LinkedHashMap<>(stall);
        response.put("status", displayBoothStatus(stall.get("status")));
        return response;
    }

    private List<Map<String, Object>> groupStallsByZone(List<Map<String, Object>> stalls) {
        Map<String, Map<String, Object>> zones = new LinkedHashMap<>();
        for (Map<String, Object> stall : stalls) {
            String zoneName = normalizeText(stall.get("zoneName"));
            if (zoneName.isEmpty()) {
                zoneName = "未分區";
            }
            Map<String, Object> zone = zones.computeIfAbsent(zoneName, key -> orderedMap(
                    "zoneName", key,
                    "zoneId", stall.get("zoneId"),
                    "stalls", new java.util.ArrayList<Map<String, Object>>()));

            Map<String, Object> stallData = new LinkedHashMap<>(stall);
            stallData.remove("zoneName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> zoneStalls = (List<Map<String, Object>>) zone.get("stalls");
            zoneStalls.add(stallData);
        }
        return List.copyOf(zones.values());
    }

    private String displayBoothStatus(Object status) {
        return switch (stringValue(status)) {
            case "AVAILABLE" -> "可選擇";
            case "SELECTED" -> "已選擇";
            case "ASSIGNED", "SOLD" -> "系統分配";
            case "DISABLED" -> "不可使用";
            default -> normalizeText(status);
        };
    }

    private boolean isSelectedStallApplication(Map<String, Object> applicationData) {
        return applicationData.get("selectedStallId") != null
                && "PAID".equals(stringValue(applicationData.get("paymentStatus")))
                && !isTrue(applicationData.get("isCancelled"))
                && stringValue(applicationData.get("refundStatus")).isEmpty();
    }

    private Map<String, Object> selectedStall(Map<String, Object> applicationData) {
        if (applicationData.get("selectedStallId") == null) {
            return null;
        }

        Map<String, Object> stall = new LinkedHashMap<>();
        stall.put("selectedStallId", applicationData.get("selectedStallId"));
        stall.put("stallNo", applicationData.get("selectedStallNo"));
        stall.put("zoneName", applicationData.get("selectedStallZoneName"));
        stall.put("width", applicationData.get("selectedStallWidth"));
        stall.put("length", applicationData.get("selectedStallLength"));
        stall.put("height", applicationData.get("selectedStallHeight"));
        return stall;
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
        if (!text.isEmpty()) {
            builder.append(text);
        }
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 1;
        }
        String text = stringValue(value);
        return "TRUE".equals(text) || "1".equals(text);
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().trim().toUpperCase();
    }

    private Map<String, Object> orderedMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return map;
    }
}
