package com.example.demo.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Service.MarketEventService;
import com.example.demo.dto.MarketEventDetailDto;

@RestController
@RequestMapping("/api/markets")
@CrossOrigin(origins = { "http://localhost:8082", "http://localhost:4200" })
public class MarketDetailController {
    private final MarketEventService marketEventService;

    public MarketDetailController(MarketEventService marketEventService) {
        this.marketEventService = marketEventService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarketEventDetailDto> getMarketEventDetail(@PathVariable Long id) {
        return marketEventService.getMarketEventDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
