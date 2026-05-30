package com.lakeserl.ai_skin_analysis_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    // Pool size = max concurrent native Mat allocations. Each analysis holds ~50-100 MB off-heap.
    // K8s tuning formula: maxPoolSize = floor(containerMemoryMB / 100) - 2 (JVM headroom).
    // Default 2/4 suits a 512 MB container. Set APP_AI_ANALYSIS_POOL_MAX_SIZE in the pod spec.
    @Value("${app.ai.analysis-pool-core-size:2}")
    private int corePoolSize;

    @Value("${app.ai.analysis-pool-max-size:4}")
    private int maxPoolSize;

    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        // Queue: 50 tasks max. Pool size (not queue size) is what caps native Mat memory.
        // Anything beyond pool + queue gets a 503 — better than OOMKilled.
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-skin-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler((task, pool) -> {
            log.error("AI task executor saturated — active={}, queue={}/{}",
                    pool.getActiveCount(), pool.getQueue().size(), pool.getQueue().remainingCapacity() + pool.getQueue().size());
            throw new RejectedExecutionException("AI analysis queue is full");
        });
        executor.initialize();
        return executor;
    }
}
