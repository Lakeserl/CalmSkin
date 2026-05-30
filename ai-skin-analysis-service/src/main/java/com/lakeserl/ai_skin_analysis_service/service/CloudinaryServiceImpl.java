package com.lakeserl.ai_skin_analysis_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.lakeserl.ai_skin_analysis_service.exception.CloudinaryUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.upload-folder:calmskin}")
    private String uploadFolder;

    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<String> uploadAsync(byte[] imageBytes, String publicId) {
        try {
            String url = upload(imageBytes, publicId);
            return CompletableFuture.completedFuture(url);
        } catch (Exception e) {
            log.error("Async Cloudinary upload failed for publicId={}: {}", publicId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            log.info("Deleting image from Cloudinary: publicId={}", uploadFolder + "/" + publicId);
            cloudinary.uploader().destroy(uploadFolder + "/" + publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete succeeded: publicId={}", publicId);
        } catch (Exception e) {
            log.error("Cloudinary delete failed for publicId={}: {} — skipping, DB record still deleted",
                    publicId, e.getMessage());
        }
    }

    @Override
    public String upload(byte[] imageBytes, String publicId) {
        try {
            log.info("Uploading image to Cloudinary: folder={}, publicId={}", uploadFolder, publicId);
            Map<?, ?> result = cloudinary.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", uploadFolder + "/" + publicId,
                    "overwrite", true,
                    "resource_type", "image"
            ));
            String url = (String) result.get("secure_url");
            log.info("Cloudinary upload successful: {}", url);
            return url;
        } catch (Exception e) {
            log.error("Cloudinary upload failed for publicId={}: {}", publicId, e.getMessage());
            throw new CloudinaryUploadException("Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }
}
