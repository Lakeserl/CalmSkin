package com.lakeserl.ai_skin_analysis_service.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${app.cloudinary.url}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(cloudinaryUrl);
    }
}
