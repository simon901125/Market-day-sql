package com.example.demo.Service;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.example.demo.Repository.RequestLogRepository;
import com.example.demo.Repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class RequestLogService {

    public static final String RESPONSE_STATUS_CODE_ATTRIBUTE = RequestLogService.class.getName() + ".responseStatusCode";
    public static final String API_RESPONSE_ATTRIBUTE = RequestLogService.class.getName() + ".apiResponse";

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogService.class);
    private static final Set<String> MUTATION_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private StatusLogService statusLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    public void recordRequest(HttpServletRequest request, int statusCode) {
        if (!isMutationApiRequest(request)) {
            return;
        }

        try {
            Long requestLogId = requestLogRepository.createRequestLog(
                    resolveUserId(request),
                    request.getMethod(),
                    request.getRequestURI(),
                    statusCode);
            if (isSuccessStatus(statusCode)) {
                statusLogService.recordForRequest(requestLogId, request);
            }
        } catch (DataAccessException exception) {
            LOGGER.warn("Failed to write request log for {} {}", request.getMethod(), request.getRequestURI(), exception);
        }
    }

    private boolean isSuccessStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private boolean isMutationApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null
                && path.startsWith("/api/")
                && MUTATION_METHODS.contains(request.getMethod());
    }

    private Long resolveUserId(HttpServletRequest request) {
        String token = jwtService.extractTokenFromAuthorizationHeader(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            return null;
        }

        String email;
        try {
            email = jwtService.getEmail(token);
        } catch (RuntimeException exception) {
            return null;
        }
        Map<String, Object> user = userRepository.findProfileByEmail(email).orElse(null);
        if (user == null || user.get("id") == null) {
            return null;
        }
        return ((Number) user.get("id")).longValue();
    }
}
