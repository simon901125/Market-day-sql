package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.StallService;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.StallSelectionRequest;
import com.example.demo.dto.response.EventStallStatusResponse;
import com.example.demo.dto.response.StallSelectionResponse;
import com.example.demo.dto.response.VendorAccountResponse;
import com.example.demo.dto.response.VendorStallMapResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "攤位選位 API", description = "提供攤主選擇活動攤位需要的資料")
public class StallController {

    @Autowired
    private StallService stallService;

    @Operation(summary = "選擇活動攤位", description = "依 eventId 與攤位編號選位，只有 AVAILABLE 攤位可被選取。")
    @PostMapping("/api/events/{eventId}/stalls/select")
    public ApiResponse<StallSelectionResponse> selectEventStall(
            @PathVariable Long eventId,
            @Valid @RequestBody StallSelectionRequest body) {
        return stallService.selectEventStall(eventId, body);
    }

    @Operation(summary = "取得活動攤位狀態", description = "依 eventId 取得該活動所有攤位目前狀態。")
    @GetMapping("/api/events/{eventId}/stallsStatus")
    public ApiResponse<List<EventStallStatusResponse>> getEventStallsStatus(@PathVariable Long eventId) {
        return stallService.getEventStallsStatus(eventId);
    }

    @Operation(summary = "取得攤主帳號資訊", description = "回傳目前登入攤主的共用資料與品牌詳細資料。")
    @GetMapping("/api/vendor/account")
    public ApiResponse<VendorAccountResponse> getVendorAccount(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return stallService.getVendorAccount(authorizationHeader);
    }

    @Operation(summary = "取得攤主選位地圖", description = "依 applicationNo 取得活動地圖、活動資訊與所有攤位狀態。")
    @GetMapping("/api/vendor/stall-map/{applicationNo}")
    public ApiResponse<VendorStallMapResponse> getVendorStallMap(@PathVariable String applicationNo) {
        return stallService.getVendorStallMap(applicationNo);
    }
}
