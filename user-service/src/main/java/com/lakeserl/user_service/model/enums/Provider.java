package com.lakeserl.user_service.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Provider {
    LOCAL,
    GOOGLE,
    FACEBOOK
}
