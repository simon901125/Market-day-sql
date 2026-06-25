package com.example.demo.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.OrganizerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "主辦方 API", description = "提供主辦方帳號與主辦資料相關功能")
public class OrganizerController {

    @Autowired
    private OrganizerService organizerService;

    @Operation(summary = "取得主辦方帳號資訊", description = "回傳目前登入主辦方的主辦方名稱、聯絡資訊、公司資訊、地址與服務時間。")
    @GetMapping("/api/organizer/account")
    public Map<String, Object> getOrganizerAccount(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return organizerService.getOrganizerAccount(authorizationHeader);
    }

    @Operation(summary = "取得主辦方報名列表", description = "依 Authorization 取得目前登入主辦方，回傳該主辦方已發布活動的所有攤位報名資料與前端顯示用申請狀態。")
    @GetMapping("/api/organizer/applications/search")
    public List<Map<String, Object>> searchOrganizerApplications(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return organizerService.searchOrganizerApplications(authorizationHeader);
    }
}
