package com.example.demo.Controller;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.example.demo.dto.response.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public GlobalResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> controllerClass = returnType.getContainingClass();
        return controllerClass == UserController.class
                || controllerClass == StallController.class
                || controllerClass == OrganizerController.class;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body instanceof ApiResponse<?> apiResponse) {
            fillSuccessMessageDetails(apiResponse, request);
            return body;
        }

        ApiResponse<Object> wrappedBody = ApiResponse.fromLegacy(body, "Success");
        fillSuccessMessageDetails(wrappedBody, request);
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(wrappedBody);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to write API response", e);
            }
        }

        return wrappedBody;
    }

    private void fillSuccessMessageDetails(ApiResponse<?> apiResponse, ServerHttpRequest request) {
        if (!apiResponse.isSuccessStatus() || apiResponse.getMessageDetails() != null) {
            return;
        }

        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        apiResponse.setMessageDetails("Executed API: " + method + " " + path);
    }
}
