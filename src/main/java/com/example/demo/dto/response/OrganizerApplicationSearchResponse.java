package com.example.demo.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "主辦方報名列表查詢結果")
public class OrganizerApplicationSearchResponse {

    @Schema(description = "符合查詢條件的報名資料總數", example = "12")
    private int totalCount;

    @Schema(description = "報名列表")
    private List<OrganizerApplicationSummaryResponse> applications;

    public OrganizerApplicationSearchResponse(List<OrganizerApplicationSummaryResponse> applications) {
        this.applications = applications;
        this.totalCount = applications == null ? 0 : applications.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<OrganizerApplicationSummaryResponse> getApplications() {
        return applications;
    }

    public void setApplications(List<OrganizerApplicationSummaryResponse> applications) {
        this.applications = applications;
    }
}
