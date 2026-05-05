package com.lakeserl.user_service.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.lakeserl.user_service.model.enums.Gender;
import com.lakeserl.user_service.model.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String phoneNumber;
    private String email;
    private String fullName;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private Gender gender;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<String> roles;
}
