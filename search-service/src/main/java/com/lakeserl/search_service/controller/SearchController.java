package com.lakeserl.search_service.controller;

import com.lakeserl.search_service.dto.ApiResponse;
import com.lakeserl.search_service.dto.SearchResponse;
import com.lakeserl.search_service.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Product search, autocomplete, and trending")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Full-text search with filters")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String skinType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SearchResponse result = searchService.search(q, category, brand, minPrice, maxPrice, skinType, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Autocomplete suggestions (< 100ms target)")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(searchService.suggest(q, Math.min(limit, 20))));
    }

    @GetMapping("/trending")
    @Operation(summary = "Top searched terms this week")
    public ResponseEntity<ApiResponse<List<String>>> trending(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(ApiResponse.ok(searchService.trending(Math.min(limit, 20))));
    }
}
