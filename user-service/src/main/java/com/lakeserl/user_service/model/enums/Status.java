package com.lakeserl.user_service.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Status {
    ACTIVE,
    INACTIVE,
    BANNED,
    UNVERIFIED
}
