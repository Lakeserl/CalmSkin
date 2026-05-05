package com.lakeserl.user_service.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OtpType {
    EMAIL_VERIFY,
    RESET_PASSWORD,
    LOGIN_OTP
}
