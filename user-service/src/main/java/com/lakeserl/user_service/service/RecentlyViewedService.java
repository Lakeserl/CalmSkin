package com.lakeserl.user_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.repository.RecentlyViewedRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecentlyViewedService {

    private static final int MAX_LIMIT = 50;

    private final RecentlyViewedRepository recentlyViewedRepository;

    @Transactional
    public void record(UUID userId, Long productId) {
        recentlyViewedRepository.upsert(userId, productId);
    }

    @Transactional(readOnly = true)
    public List<Long> list(UUID userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        return recentlyViewedRepository.findProductIdsByUserId(userId, PageRequest.of(0, safeLimit));
    }
}
