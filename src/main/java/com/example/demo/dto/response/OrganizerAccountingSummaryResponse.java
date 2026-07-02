package com.example.demo.dto.response;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "主辦方帳務列表單筆資料")
public class OrganizerAccountingSummaryResponse extends MapBackedResponse {

    public OrganizerAccountingSummaryResponse(Map<String, Object> values) {
        super(values);
    }
}
