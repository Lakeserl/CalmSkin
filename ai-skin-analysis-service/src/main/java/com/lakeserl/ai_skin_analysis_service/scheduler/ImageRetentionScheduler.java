package com.lakeserl.ai_skin_analysis_service.scheduler;

import com.lakeserl.ai_skin_analysis_service.entity.SkinAnalysisSession;
import com.lakeserl.ai_skin_analysis_service.repository.SkinAnalysisSessionRepository;
import com.lakeserl.ai_skin_analysis_service.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageRetentionScheduler {

    private final SkinAnalysisSessionRepository sessionRepository;
    private final CloudinaryService cloudinaryService;

    // Decree 13/2023 data minimization: processed (normalized) face images must not be kept
    // longer than necessary. Original images are never stored on Cloudinary — the processor
    // uploads only the OpenCV-normalized image (EXIF-stripped). This scheduler handles
    // the processed image TTL only. Default 30 days; override via IMAGE_RETENTION_DAYS.
    @Value("${app.image.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredImages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<SkinAnalysisSession> expired =
                sessionRepository.findTop100ByProcessedImageUrlIsNotNullAndCreatedAtBefore(cutoff);

        if (expired.isEmpty()) {
            log.debug("Retention purge: no expired images (cutoff={})", cutoff);
            return;
        }

        log.info("Retention purge: {} sessions older than {} days — deleting Cloudinary images",
                expired.size(), retentionDays);

        int deleted = 0;
        for (SkinAnalysisSession session : expired) {
            try {
                cloudinaryService.delete(session.getSessionId() + "-processed");
                session.setProcessedImageUrl(null);
                sessionRepository.save(session);
                deleted++;
            } catch (Exception e) {
                log.error("Retention purge failed for sessionId={}: {}", session.getSessionId(), e.getMessage());
            }
        }

        log.info("Retention purge complete: {}/{} sessions cleared", deleted, expired.size());
    }
}
