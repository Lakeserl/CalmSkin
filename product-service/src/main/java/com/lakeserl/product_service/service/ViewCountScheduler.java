package com.lakeserl.product_service.service;

import com.lakeserl.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductRepository productRepository;
    
    private static final String VIEW_COUNT_KEY_PREFIX = "product:view:";
    private static final String VIEW_COUNT_PATTERN = "product:view:*";

    /**
     * Runs every hour to flush view counts from Redis to the database
     */
    @Scheduled(fixedRateString = "${app.scheduling.view-count-flush:3600000}")
    @Transactional
    public void flushViewCountsToDatabase() {
        log.info("Starting view count flush to database");
        
        List<String> keysToDelete = new ArrayList<>();
        
        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory().getConnection()
                .keyCommands().scan(ScanOptions.scanOptions().match(VIEW_COUNT_PATTERN).count(100).build())) {
            
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                Long productId = extractProductId(key);
                
                if (productId != null) {
                    Object countObj = redisTemplate.opsForValue().get(key);
                    if (countObj != null) {
                        try {
                            Long viewCount = Long.parseLong(countObj.toString());
                            
                            if (viewCount > 0) {
                                productRepository.findById(productId).ifPresent(product -> {
                                    product.setViewCount(product.getViewCount() + viewCount);
                                    productRepository.save(product);
                                });
                            }
                        } catch (NumberFormatException e) {
                            log.error("Invalid view count value for key {}: {}", key, countObj);
                        }
                    }
                }
                
                keysToDelete.add(key);
            }
            
            // Delete processed keys
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("Flushed {} view counts to database", keysToDelete.size());
            }
            
        } catch (Exception e) {
            log.error("Error during view count flush", e);
        }
    }
    
    private Long extractProductId(String key) {
        try {
            return Long.parseLong(key.substring(VIEW_COUNT_KEY_PREFIX.length()));
        } catch (Exception e) {
            return null;
        }
    }
}
