package com.example.demo.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "主辦方帳號資料")
public class OrganizerAccountResponse {

    private Map<String, Object> values;

    public OrganizerAccountResponse(Map<String, Object> values) {
        this.values = values;
    }

    @JsonAnyGetter
    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
}
