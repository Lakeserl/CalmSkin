package com.lakeserl.user_service.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100)
    private String recipientName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^(\\+84|84|0)[0-9]{9,10}$", message = "Invalid phone format")
    private String phone;

    @NotBlank(message = "Province is required")
    private String province;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "Street is required")
    private String street;

    private String postalCode;

    private boolean isDefault;
}
