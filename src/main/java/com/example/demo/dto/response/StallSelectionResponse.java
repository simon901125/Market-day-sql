package com.example.demo.dto.response;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "攤位選位結果")
public class StallSelectionResponse {

    private String applicationNo;
    private String stallNo;
    private List<Selection> selections;

    public StallSelectionResponse() {
    }

    public StallSelectionResponse(String applicationNo, String stallNo) {
        this.applicationNo = applicationNo;
        this.stallNo = stallNo;
    }

    public StallSelectionResponse(String applicationNo, List<Selection> selections) {
        this.applicationNo = applicationNo;
        this.selections = selections;
        this.stallNo = selections == null || selections.isEmpty() ? null : selections.get(0).getStallNo();
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

    public List<Selection> getSelections() {
        return selections;
    }

    public void setSelections(List<Selection> selections) {
        this.selections = selections;
    }

    public static class Selection {

        private LocalDate applyDate;
        private String stallNo;

        public Selection() {
        }

        public Selection(LocalDate applyDate, String stallNo) {
            this.applyDate = applyDate;
            this.stallNo = stallNo;
        }

        public LocalDate getApplyDate() {
            return applyDate;
        }

        public void setApplyDate(LocalDate applyDate) {
            this.applyDate = applyDate;
        }

        public String getStallNo() {
            return stallNo;
        }

        public void setStallNo(String stallNo) {
            this.stallNo = stallNo;
        }
    }
}
