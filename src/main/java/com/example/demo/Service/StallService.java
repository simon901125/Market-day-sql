package com.example.demo.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.example.demo.Repository.StallRepository;
import com.example.demo.dto.request.StallSelectionRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.EventStallStatusResponse;
import com.example.demo.dto.response.StallSelectionResponse;
import com.example.demo.dto.response.VendorAccountResponse;
import com.example.demo.dto.response.VendorStallMapResponse;

@Service
public class StallService {

    @Autowired
    private StallRepository stallRepository;

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

        Map<String, Object> application = stallRepository.findSelectableApplication(body.getApplicationNo())
                .orElse(null);
        if (application == null) {
            return ApiResponse.fail("Application is not approved, paid, or selectable");
        }

        Long vendorUserId = ((Number) vendor.get("userId")).longValue();
        Long applicationUserId = ((Number) application.get("userId")).longValue();
        if (!vendorUserId.equals(applicationUserId)) {
            return ApiResponse.fail("Application does not belong to this account");
        }

        Long eventId = ((Number) application.get("eventId")).longValue();
        Long stallId = stallRepository.findStallId(eventId, body.getStallNo())
                .orElse(null);
        if (stallId == null) {
            return ApiResponse.fail("Stall is not available");
        }

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
            return ApiResponse.fail("Application binding failed");
        }

        return ApiResponse.success(
                "Stall selection successful",
                new StallSelectionResponse(body.getApplicationNo(), body.getStallNo()));
    }

    public ApiResponse<List<EventStallStatusResponse>> getEventStallsStatus(Long eventId) {
        List<EventStallStatusResponse> stalls = stallRepository.findEventStallsStatus(eventId).stream()
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
        List<Map<String, Object>> stalls = stallRepository.findEventStallsMap(eventId);

        Map<String, Object> application = new LinkedHashMap<>();
        application.put("applicationNo", applicationData.get("applicationNo"));
        application.put("applicationStatus", applicationStatus);
        application.put("vendorName", applicationData.get("vendorName"));
        application.put("selectedStallId", applicationData.get("selectedStallId"));
        application.put("selectedStall", selectedStall(applicationData));

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventTitle", applicationData.get("eventTitle"));
        event.put("startDate", applicationData.get("startDate"));
        event.put("endDate", applicationData.get("endDate"));
        event.put("address", joinAddress(
                applicationData.get("city"),
                applicationData.get("district"),
                applicationData.get("address")));

        return ApiResponse.success("Vendor stall map retrieved successfully", new VendorStallMapResponse(application, event, stalls));
    }

    private boolean canViewStallMap(String applicationStatus, Map<String, Object> applicationData) {
        return "待選位".equals(applicationStatus) || isSelectedStallApplication(applicationData);
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
        if (value == null) {
            return;
        }
        String text = value.toString().trim();
        if (!text.isEmpty()) {
            builder.append(text);
        }
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
}
