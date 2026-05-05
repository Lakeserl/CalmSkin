package com.lakeserl.user_service.model.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private UUID id;
    private String recipientName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String postalCode;
    private boolean isDefault;
}
