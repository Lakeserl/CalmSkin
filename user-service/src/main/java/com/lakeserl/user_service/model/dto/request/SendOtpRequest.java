package com.lakeserl.user_service.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+84[0-9]{9,10}$|^0[0-9]{9,10}$", message = "Invalid phone format")
    private String phoneNumber;
}
