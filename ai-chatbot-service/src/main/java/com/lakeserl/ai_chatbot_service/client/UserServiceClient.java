package com.lakeserl.ai_chatbot_service.client;

import com.lakeserl.ai_chatbot_service.dto.ApiResponse;
import com.lakeserl.ai_chatbot_service.dto.UserSkinProfileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserServiceClient {

    @GetMapping("/skin-profiles/{userId}")
    ApiResponse<UserSkinProfileDTO> getSkinProfile(
            @PathVariable UUID userId,
            @RequestHeader("X-Internal-Secret") String secret);
}
