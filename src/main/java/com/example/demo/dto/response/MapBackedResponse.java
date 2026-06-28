package com.example.demo.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

public class MapBackedResponse {

    private Map<String, Object> values;

    public MapBackedResponse(Map<String, Object> values) {
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
