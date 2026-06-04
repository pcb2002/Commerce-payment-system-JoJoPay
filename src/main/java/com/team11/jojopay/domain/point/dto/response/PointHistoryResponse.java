package com.team11.jojopay.domain.point.dto.response;

import com.team11.jojopay.domain.point.entity.PointHistory;
import com.team11.jojopay.domain.point.enums.PointTransactionType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PointHistoryResponse {

    private final Long id;
    private final PointTransactionType transactionType;
    private final String transactionTypeDescription;
    private final Long amount;
    private final LocalDateTime createdAt;

    public PointHistoryResponse(PointHistory history) {
        this.id = history.getId();
        this.transactionType = history.getTransactionType();
        this.transactionTypeDescription = history.getTransactionType().getDescription();
        this.amount = history.getAmount();
        this.createdAt = history.getCreatedAt();
    }
}
