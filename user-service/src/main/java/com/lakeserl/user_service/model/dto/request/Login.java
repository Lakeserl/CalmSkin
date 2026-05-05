package com.lakeserl.user_service.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Login {
    
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
