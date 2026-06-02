package com.lakeserl.ai_chatbot_service.service;

import com.lakeserl.ai_chatbot_service.client.UserServiceClient;
import com.lakeserl.ai_chatbot_service.dto.ApiResponse;
import com.lakeserl.ai_chatbot_service.dto.UserSkinProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserServiceClient userServiceClient;

    @Value("${app.internal.secret}")
    private String internalSecret;

    public String buildUserContext(UUID userId) {
        try {
            ApiResponse<UserSkinProfileDTO> response = userServiceClient.getSkinProfile(userId, internalSecret);
            if (response == null || response.getData() == null) {
                return "";
            }
            UserSkinProfileDTO profile = response.getData();
            StringBuilder sb = new StringBuilder("User skin profile: ");
            if (profile.getSkinType() != null) {
                sb.append("skin type=").append(profile.getSkinType()).append("; ");
            }
            if (profile.getSkinConcerns() != null && !profile.getSkinConcerns().isEmpty()) {
                sb.append("concerns=").append(String.join(", ", profile.getSkinConcerns())).append("; ");
            }
            if (profile.getAllergies() != null && !profile.getAllergies().isEmpty()) {
                sb.append("allergies=").append(String.join(", ", profile.getAllergies())).append("; ");
            }
            if (profile.getNote() != null && !profile.getNote().isBlank()) {
                sb.append("note: ").append(profile.getNote());
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.debug("Could not fetch user skin profile for userId={}: {}", userId, e.getMessage());
            return "";
        }
    }
}
