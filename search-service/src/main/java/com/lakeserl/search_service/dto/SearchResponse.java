package com.lakeserl.search_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    private List<SearchResultDTO> results;
    private long totalHits;
    private int page;
    private int size;
    private int totalPages;
}
