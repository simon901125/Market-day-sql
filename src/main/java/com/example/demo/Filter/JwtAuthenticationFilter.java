package com.example.demo.Filter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.Service.JwtService;
import com.example.demo.Service.UpdateActiveTimeService;
import com.example.demo.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UpdateActiveTimeService updateActiveTimeService;
    private final ObjectMapper objectMapper;

    //api放置處
    private final Set<ProtectedApi> protectedApis = Set.of(
            new ProtectedApi(HttpMethod.POST.name(), "/api/auth/logout"),
            new ProtectedApi(HttpMethod.GET.name(), "/api/auth/me"),
            new ProtectedApi(HttpMethod.POST.name(), "/api/users/me"),
            new ProtectedApi(HttpMethod.POST.name(), "/api/account/deactivate"),
            new ProtectedApi(HttpMethod.GET.name(), "/api/vendor/account"),
            new ProtectedApi(HttpMethod.GET.name(), "/api/organizer/account"));

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UpdateActiveTimeService updateActiveTimeService,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.updateActiveTimeService = updateActiveTimeService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        //非目標apis直接放行
        if (!isProtectedApi(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        //目標api，做驗證
        String token = jwtService.extractTokenFromAuthorizationHeader(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            writeUnauthorizedResponse(response, "Authorization token is required");
            return;
        }

        if (!jwtService.isTokenValid(token)) {
            writeUnauthorizedResponse(response, "Invalid or expired token");
            return;
        }

        if (!updateActiveTimeService.refreshActiveTimeByToken(token)) {
            writeUnauthorizedResponse(response, "Session expired");
            return;
        }

        filterChain.doFilter(request, response);
    }
    //將目前的請求包裝成:ProtectedApi("GET", "/api/auth/me")，來做後續比對
    private boolean isProtectedApi(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return protectedApis.contains(new ProtectedApi(method, path));
    }
    //編寫錯誤回報
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(HttpServletResponse.SC_UNAUTHORIZED, message));
    }

    private record ProtectedApi(String method, String path) {
    }
}

