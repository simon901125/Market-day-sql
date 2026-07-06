package com.example.demo.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.MarketEventService;
import com.example.demo.dto.request.MarketSearchRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.MarketEventCardResponse;
import com.example.demo.dto.response.MarketEventDetailResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Market API", description = "Market event search and detail APIs")
public class MarketController {

    @Autowired
    private MarketEventService marketEventService;

    @Operation(summary = "Search market events", description = "Search published and approved market events by keyword, city, event status, date range, category, and event type.")
    @PostMapping("/api/markets/search")
    public ApiResponse<List<MarketEventCardResponse>> searchMarkets(
            @RequestBody MarketSearchRequest request) {
        return marketEventService.searchMarkets(request);
    }

    @Operation(summary = "Get market event detail", description = "Get detail for a published and approved market event by event id.")
    @GetMapping("/api/markets/{id}")
    public ApiResponse<MarketEventDetailResponse> getMarketDetail(@PathVariable Long id) {
        return marketEventService.getMarketDetail(id);
    }
}
