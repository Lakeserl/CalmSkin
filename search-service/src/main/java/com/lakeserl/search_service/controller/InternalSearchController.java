package com.lakeserl.search_service.controller;

import com.lakeserl.search_service.dto.ApiResponse;
import com.lakeserl.search_service.service.SearchService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/search")
@RequiredArgsConstructor
@Hidden
public class InternalSearchController {

    private final SearchService searchService;

    @PostMapping("/reindex")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> reindex() {
        int count = searchService.reindexAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("indexed", count)));
    }
}
