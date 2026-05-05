package com.lakeserl.user_service.model.dto.request;

import java.time.LocalDate;

import com.lakeserl.user_service.model.enums.Gender;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Full name must be 2-50 characters")
    private String fullName;

    @Size(min = 10, max = 11)
    private String phoneNumber;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;
}
