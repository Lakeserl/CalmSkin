package com.lakeserl.review_service.service;

import com.lakeserl.review_service.dto.request.PresignRequest;
import com.lakeserl.review_service.dto.response.PresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaPresignService {

    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket:calmskin-review-media}")
    private String bucket;

    @Value("${app.s3.cdn-base-url:}")
    private String cdnBaseUrl;

    public PresignResponse presign(UUID userId, PresignRequest request) {
        String ext = request.filename().contains(".") ?
                request.filename().substring(request.filename().lastIndexOf('.')) : "";
        String key = "reviews/" + userId + "/" + UUID.randomUUID() + ext;

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(request.contentType())
                        .build()));

        String mediaUrl = cdnBaseUrl.isBlank() ?
                "https://" + bucket + ".s3.amazonaws.com/" + key :
                cdnBaseUrl + "/" + key;

        return PresignResponse.builder()
                .uploadUrl(presigned.url().toString())
                .mediaUrl(mediaUrl)
                .build();
    }
}
