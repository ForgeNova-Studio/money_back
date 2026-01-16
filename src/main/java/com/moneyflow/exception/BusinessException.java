package com.moneyflow.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외
 *
 * ErrorCode를 통해 표준화된 에러 응답 제공
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    // 기존 생성자 유지 (하위 호환)
    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.INVALID_INPUT;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
    }
}
