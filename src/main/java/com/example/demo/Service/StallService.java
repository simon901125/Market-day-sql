package com.example.demo.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.example.demo.Repository.StallRepository;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.StallSelectionRequest;
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

    @Transactional
    public ApiResponse<StallSelectionResponse> selectEventStall(Long eventId, StallSelectionRequest body) {
        if (body.getApplicationNo() == null || body.getApplicationNo().isBlank()) {
            return ApiResponse.fail("Application number is required");
        }

        if (body.getStallNo() == null || body.getStallNo().isBlank()) {
            return ApiResponse.fail("Stall number is required");
        }

        //確認位置狀態
        Long stallId = stallRepository.findAvailableStallId(eventId, body.getStallNo())
                .orElse(null);
        if (stallId == null) {
            return ApiResponse.fail("Stall is not available");
        }

        //確保狀態為可選位階段(未付款、未審核、位置已有選擇皆不可選)
        if (stallRepository.findSelectableApplication(eventId, body.getApplicationNo()).isEmpty()) {
            return ApiResponse.fail("Application is not approved, paid, or selectable");
        }

        //連結申請單當中的selected_stall_id與event_stalls
        int applicationUpdatedRows = stallRepository.bindApplicationSelectedStall(
                eventId,
                body.getApplicationNo(),
                stallId);
        if (applicationUpdatedRows == 0) {
            return ApiResponse.fail("Application binding failed");
        }

        //將選擇的位子狀態修正
        int stallUpdatedRows = stallRepository.selectAvailableStall(stallId);
        if (stallUpdatedRows == 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.fail("Stall selection failed");
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
        account.put("address", vendor.get("address"));
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

    public ApiResponse<VendorStallMapResponse> getVendorStallMap(String applicationNo) {
        if (applicationNo == null || applicationNo.isBlank()) {
            return ApiResponse.fail("Application number is required");
        }

        Map<String, Object> applicationData = stallRepository.findVendorStallMapApplication(applicationNo)
                .orElse(null);
        if (applicationData == null) {
            return ApiResponse.fail("Application not found");
        }

        Long eventId = ((Number) applicationData.get("eventId")).longValue();
        List<Map<String, Object>> stalls = stallRepository.findEventStallsMap(eventId);

        Map<String, Object> application = new LinkedHashMap<>();
        application.put("applicationNo", applicationData.get("applicationNo"));
        application.put("vendorName", applicationData.get("vendorName"));
        application.put("selectedStallId", applicationData.get("selectedStallId"));

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventTitle", applicationData.get("eventTitle"));
        event.put("startDate", applicationData.get("startDate"));
        event.put("endDate", applicationData.get("endDate"));
        event.put("address", applicationData.get("address"));

        return ApiResponse.success("Vendor stall map retrieved successfully", new VendorStallMapResponse(application, event, stalls));
    }
}


