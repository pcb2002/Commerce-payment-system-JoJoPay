package com.team11.jojopay.common.exception;

import lombok.Getter;
import com.team11.jojopay.common.exception.ErrorCode;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}