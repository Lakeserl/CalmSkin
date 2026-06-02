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

    // Pool size = max concurrent off-heap Mat allocations (~100 MB each from OpenCV).
    // Mat memory is OUTSIDE -Xmx — container limit must hold heap AND native simultaneously.
    // Correct K8s tuning formula (solve for N):
    //   N = (containerMB - Xmx - 300) / 100
    // 300 MB = JVM baseline: metaspace + thread stacks + code cache + Kafka/JPA/misc buffers.
    // Example: 2 GB container, -Xmx 1 GB → (2048 - 1024 - 300) / 100 ≈ 7 → set max=7.
    // Default core=2 / max=4 is safe for a 1 GB container with -Xmx 512 MB: (1024-512-300)/100 ≈ 2.
    // Set APP_AI_ANALYSIS_POOL_MAX_SIZE in the pod spec; must be computed against actual -Xmx.
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
