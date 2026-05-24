package com.lakeserl.user_service.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class UpdateAvatarRequest {

    @NotBlank(message = "Avatar URL must not be blank")
    @URL(message = "Avatar URL must be a valid URL")
    private String avatarUrl;

    public UpdateAvatarRequest() {}

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
