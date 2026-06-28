package com.example.demo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "攤位選位成功回應")
public class StallSelectionResponse {

    private String applicationNo;
    private String stallNo;

    public StallSelectionResponse(String applicationNo, String stallNo) {
        this.applicationNo = applicationNo;
        this.stallNo = stallNo;
    }

    public String getApplicationNo() {
        return applicationNo;
    }

    public void setApplicationNo(String applicationNo) {
        this.applicationNo = applicationNo;
    }

    public String getStallNo() {
        return stallNo;
    }

    public void setStallNo(String stallNo) {
        this.stallNo = stallNo;
    }
}
