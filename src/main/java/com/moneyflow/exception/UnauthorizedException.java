package com.moneyflow.exception;

import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {
    private final ErrorCode errorCode;

    public UnauthorizedException(String message) {
        this(message, ErrorCode.AUTHENTICATION_ERROR);
    }

    public UnauthorizedException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public static UnauthorizedException authentication(String message) {
        return new UnauthorizedException(message, ErrorCode.AUTHENTICATION_ERROR);
    }

    public static UnauthorizedException accessDenied(String message) {
        return new UnauthorizedException(message, ErrorCode.ACCESS_DENIED);
    }
}
