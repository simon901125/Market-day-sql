package com.example.demo.dto.response;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "主辦方報名列表項目")
public class OrganizerApplicationSummaryResponse extends MapBackedResponse {

    public OrganizerApplicationSummaryResponse(Map<String, Object> values) {
        super(values);
    }
}
