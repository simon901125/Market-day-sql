package com.example.demo.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Repository.OrganizerRepository;

@Service
public class OrganizerService {

    private static final DateTimeFormatter SERVICE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ApplicationStatusService applicationStatusService;

    public Map<String, Object> getOrganizerAccount(String authorizationHeader) {
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

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("organizerName", organizer.get("organizerName"));
        account.put("contactName", organizer.get("contactName"));
        account.put("contactPhone", organizer.get("contactPhone"));
        account.put("contactEmail", organizer.get("contactEmail"));
        account.put("companyName", organizer.get("companyName"));
        account.put("taxId", organizer.get("taxId"));
        account.put("city", organizer.get("city"));
        account.put("district", organizer.get("district"));
        account.put("address", organizer.get("address"));
        account.put("serviceDays", organizer.get("serviceDays"));
        account.put("serviceTime", formatServiceTime(
                organizer.get("serviceStartTime"),
                organizer.get("serviceEndTime")));
        return account;
    }

    public List<Map<String, Object>> searchOrganizerApplications(String authorizationHeader) {
        Map<String, Object> organizer = getAuthenticatedOrganizer(authorizationHeader);
        if (organizer.containsKey("message")) {
            return List.of(organizer);
        }

        Long organizerUserId = ((Number) organizer.get("userId")).longValue();
        return organizerRepository.findOrganizerApplications(organizerUserId).stream()
                .map(this::withDisplayApplicationStatus)
                .toList();
    }

    private Map<String, Object> withDisplayApplicationStatus(Map<String, Object> application) {
        Map<String, Object> response = new LinkedHashMap<>(application);
        response.put("applicationStatus", applicationStatusService.resolveApplicationStatus(application));
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
}
