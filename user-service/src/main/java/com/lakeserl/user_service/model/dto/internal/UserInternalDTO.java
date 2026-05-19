package com.lakeserl.user_service.model.dto.internal;

import java.util.UUID;

import com.lakeserl.user_service.model.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInternalDTO {
    private UUID id;
    private String email;
    private String fullName;
    private Status status;
}
