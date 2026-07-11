package com.example.demo.dto.response;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "攤主選位地圖")
public class VendorStallMapResponse {

    private Map<String, Object> application;
    private Map<String, Object> event;
    private List<Map<String, Object>> stalls;

    public VendorStallMapResponse(
            Map<String, Object> application,
            Map<String, Object> event,
            List<Map<String, Object>> stalls) {
        this.application = application;
        this.event = event;
        this.stalls = stalls;
    }

    public Map<String, Object> getApplication() {
        return application;
    }

    public void setApplication(Map<String, Object> application) {
        this.application = application;
    }

    public Map<String, Object> getEvent() {
        return event;
    }

    public void setEvent(Map<String, Object> event) {
        this.event = event;
    }

    public List<Map<String, Object>> getStalls() {
        return stalls;
    }

    public void setStalls(List<Map<String, Object>> stalls) {
        this.stalls = stalls;
    }
}
