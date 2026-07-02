package com.example.demo.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "主辦方帳務列表查詢結果")
public class OrganizerAccountingSearchResponse {

    @Schema(description = "帳務列表筆數", example = "12")
    private int totalCount;

    @Schema(description = "帳務列表")
    private List<OrganizerAccountingSummaryResponse> accounts;

    public OrganizerAccountingSearchResponse(List<OrganizerAccountingSummaryResponse> accounts) {
        this.accounts = accounts;
        this.totalCount = accounts == null ? 0 : accounts.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<OrganizerAccountingSummaryResponse> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<OrganizerAccountingSummaryResponse> accounts) {
        this.accounts = accounts;
    }
}
