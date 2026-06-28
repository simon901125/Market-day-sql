package com.example.demo.dto.response;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "活動攤位狀態")
public class EventStallStatusResponse extends MapBackedResponse {

    public EventStallStatusResponse(Map<String, Object> values) {
        super(values);
    }
}
