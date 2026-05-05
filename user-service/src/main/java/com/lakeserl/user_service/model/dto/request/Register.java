package com.lakeserl.user_service.model.dto.request;

import java.time.LocalDate;
import java.util.Set;

import com.lakeserl.user_service.model.enums.Gender;

import jakarta.validation.constraints.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
public class Register {
    
    @NotBlank(message = "Adding fullname")
    @Size(min = 6, max = 50, message = "Fullname must be at least 6 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]{6,30}@gmail\\.com$", message = "Only Gmail addresses are allowed")
    @NotBlank(message = "Email is required")
    @Size(max = 50, message = "Email must be less than 50 characters")
    private String email;

    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 digits")
    @Pattern(regexp = "^(\\+84|84|0)[0-9]{9,10}$", message = "The phone number is not in the correct format")
    private String phoneNumber;

    @Pattern(regexp = "^(http|https)://.*$", message = "Avatar URL must be a valid HTTP or HTTPS URL")
    private String avatarUrl;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(?!.*\\s)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d])[^\\s]{10,128}$", message = "Password too weak")
    @NotBlank(message = "Password is required")
    private String password;

    @NotNull
    private Gender gender;

    private Set<String> roles;
}
