package com.team11.jojopay.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointTransactionType {
    EARN("적립"),
    USE("사용"),
    USE_RECOVERY("사용복구"),
    EARN_FORFEIT("적립회수");

    private final String description;
}
