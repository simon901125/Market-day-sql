package com.example.demo.dto.response;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "主辦方報名詳情")
public class OrganizerApplicationDetailResponse extends MapBackedResponse {

    public OrganizerApplicationDetailResponse(Map<String, Object> values) {
        super(values);
    }
}
