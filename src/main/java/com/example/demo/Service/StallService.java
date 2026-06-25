package com.example.demo.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.example.demo.Repository.StallRepository;
import com.example.demo.dto.StallSelectionRequest;

@Service
public class StallService {

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private JwtService jwtService;

    @Transactional
    public Map<String, Object> selectEventStall(Long eventId, StallSelectionRequest body) {
        if (body.getApplicationNo() == null || body.getApplicationNo().isBlank()) {
            return Map.of("message", "Application number is required");
        }

        if (body.getStallNo() == null || body.getStallNo().isBlank()) {
            return Map.of("message", "Stall number is required");
        }

        //確認位置狀態
        Long stallId = stallRepository.findAvailableStallId(eventId, body.getStallNo())
                .orElse(null);
        if (stallId == null) {
            return Map.of("message", "Stall is not available");
        }

        //確保狀態為可選位階段(未付款、未審核、位置已有選擇皆不可選)
        if (stallRepository.findSelectableApplication(eventId, body.getApplicationNo()).isEmpty()) {
            return Map.of("message", "Application is not approved, paid, or selectable");
        }

        //連結申請單當中的selected_stall_id與event_stalls
        int applicationUpdatedRows = stallRepository.bindApplicationSelectedStall(
                eventId,
                body.getApplicationNo(),
                stallId);
        if (applicationUpdatedRows == 0) {
            return Map.of("message", "Application binding failed");
        }

        //將選擇的位子狀態修正
        int stallUpdatedRows = stallRepository.selectAvailableStall(stallId);
        if (stallUpdatedRows == 0) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Map.of("message", "Stall selection failed");
        }

        return Map.of(
                "message", "Stall selection successful",
                "applicationNo", body.getApplicationNo(),
                "stallNo", body.getStallNo());
    }

    public List<Map<String, Object>> getEventStallsStatus(Long eventId) {
        return stallRepository.findEventStallsStatus(eventId);
    }
    public Map<String, Object> getVendorAccount(String authorizationHeader) {
        String token = jwtService.extractTokenFromAuthorizationHeader(authorizationHeader);
        if (token == null || token.isBlank()) {
            return Map.of("message", "Authorization token is required");
        }
        if (!jwtService.isTokenValid(token)) {
            return Map.of("message", "Invalid or expired token");
        }

        String email = jwtService.getEmail(token);
        Map<String, Object> vendor = stallRepository.findVendorAccountByEmail(email)
                .orElse(null);
        if (vendor == null) {
            return Map.of("message", "Vendor profile not found");
        }
        if (!"VENDOR".equals(vendor.get("role"))) {
            return Map.of("message", "This account is not a vendor");
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
        return account;
    }

    public Map<String, Object> getVendorStallMap(String applicationNo) {
        if (applicationNo == null || applicationNo.isBlank()) {
            return Map.of("message", "Application number is required");
        }

        Map<String, Object> applicationData = stallRepository.findVendorStallMapApplication(applicationNo)
                .orElse(null);
        if (applicationData == null) {
            return Map.of("message", "Application not found");
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
        event.put("mapImageUrl", applicationData.get("mapImageUrl"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", application);
        response.put("event", event);
        response.put("stalls", stalls);
        return response;
    }
}


