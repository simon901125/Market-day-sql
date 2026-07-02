package com.example.demo.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.StallService;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.EventStallStatusResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "公開 API", description = "不需要特別權限即可使用的公開查詢 API")
public class AllController {

    @Autowired
    private StallService stallService;

    @Operation(summary = "取得公開活動攤位狀態", description = "依活動 ID 取得指定日期的攤位狀態；未帶 applyDate 時預設回傳活動第一天。")
    @GetMapping("/api/eventsMap/{eventId}/stallsStatus")
    public ApiResponse<List<EventStallStatusResponse>> getPublicEventStallsStatus(
            @Parameter(description = "活動 ID", example = "1")
            @PathVariable Long eventId,
            @Parameter(description = "查詢日期，格式為 yyyy-MM-dd；未提供時預設活動第一天", example = "2026-08-01")
            @RequestParam(value = "applyDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applyDate) {
        return stallService.getPublicEventStallsStatus(eventId, applyDate);
    }
}
