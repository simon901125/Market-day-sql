package com.example.demo.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "攤主帳號資料")
public class VendorAccountResponse {

    private Map<String, Object> values;

    public VendorAccountResponse(Map<String, Object> values) {
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
