package com.lakeserl.ai_skin_analysis_service.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.ai_skin_analysis_service.dto.response.RecommendedProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    @Value("${app.services.product-service-url:http://product-service}")
    private String productServiceUrl;

    @Value("${app.internal.secret}")
    private String internalSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public List<RecommendedProductDTO> findBySkinProfile(String skinType, List<String> concerns) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(productServiceUrl + "/internal/products/by-skin-profile")
                    .queryParam("skinType", skinType)
                    .queryParam("concerns", concerns != null ? String.join(",", concerns) : "")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Secret", internalSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            var response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                Object data = ((Map<?, ?>) response.getBody()).get("data");
                if (data != null) {
                    return objectMapper.convertValue(data, new TypeReference<List<RecommendedProductDTO>>() {});
                }
            }
        } catch (Exception e) {
            log.warn("Product service call failed for skinType={}: {} — returning empty list",
                    skinType, e.getMessage());
        }
        return List.of();
    }
}
