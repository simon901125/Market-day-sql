package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.OrganizerService;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.OrganizerAccountResponse;
import com.example.demo.dto.response.OrganizerApplicationDetailResponse;
import com.example.demo.dto.response.OrganizerApplicationSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "主辦方 API", description = "提供主辦方帳號與主辦資料相關功能")
public class OrganizerController {

    @Autowired
    private OrganizerService organizerService;

    @Operation(summary = "取得主辦方帳號資訊", description = "回傳目前登入主辦方的主辦方名稱、聯絡資訊、公司資訊、地址與服務時間。")
    @GetMapping("/api/organizer/account")
    public ApiResponse<OrganizerAccountResponse> getOrganizerAccount(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return organizerService.getOrganizerAccount(authorizationHeader);
    }

    @Operation(summary = "查詢主辦方報名列表", description = "依 Authorization 取得目前登入主辦方，並依活動名稱、狀態、品牌名稱、報名時間區間篩選報名資料。")
    @GetMapping("/api/organizer/applications/search")
    public ApiResponse<List<OrganizerApplicationSummaryResponse>> searchOrganizerApplications(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return organizerService.searchOrganizerApplications(authorizationHeader);
    }

    @Operation(summary = "取得主辦方報名詳情", description = "依報名 ID 取得目前登入主辦方活動底下的單筆報名詳細資料。")
    @GetMapping("/api/organizer/applications/{id}")
    public ApiResponse<OrganizerApplicationDetailResponse> getOrganizerApplicationDetail(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long id) {
        return organizerService.getOrganizerApplicationDetail(authorizationHeader, id);
    }
}
