package com.lakeserl.ai_skin_analysis_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    @Value("${app.services.user-service-url:http://user-service}")
    private String userServiceUrl;

    @Value("${app.internal.secret}")
    private String internalSecret;

    private final RestTemplate restTemplate;

    public boolean userExists(Long userId) {
        try {
            String url = userServiceUrl + "/internal/users/" + userId + "/exists";
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            var entity = new org.springframework.http.HttpEntity<>(headers);
            restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Void.class);
            return true;
        } catch (Exception e) {
            log.warn("Could not verify user {} existence: {}", userId, e.getMessage());
            return true; // fail open — don't block analysis if user-service is down
        }
    }
}
