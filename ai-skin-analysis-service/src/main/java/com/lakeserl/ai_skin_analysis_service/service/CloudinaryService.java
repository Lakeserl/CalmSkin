package com.lakeserl.ai_skin_analysis_service.service;

import java.util.concurrent.CompletableFuture;

public interface CloudinaryService {

    CompletableFuture<String> uploadAsync(byte[] imageBytes, String publicId);

    String upload(byte[] imageBytes, String publicId);

    /**
     * Permanently deletes the image from Cloudinary storage.
     * Required by Decree 13/2023 art. 16: biometric data must be erasable on user request.
     * Failures are logged but do not propagate — DB deletion proceeds regardless.
     */
    void delete(String publicId);
}
