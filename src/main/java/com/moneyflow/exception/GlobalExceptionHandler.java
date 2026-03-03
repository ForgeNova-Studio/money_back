package com.moneyflow.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 글로벌 예외 핸들러
 *
 * 모든 예외를 JSON 형식으로 응답하여 API 클라이언트가 안정적으로 에러 처리 가능
 *
 * ⚠️ basePackages 설정:
 * - com.moneyflow 패키지의 컨트롤러에서 발생한 예외만 처리
 * - Actuator, Spring MVC 표준 응답(404, 405 등)이 정상 동작하도록 보장
 */
@RestControllerAdvice(basePackages = "com.moneyflow")
@Slf4j
public class GlobalExceptionHandler {

    // ===== 비즈니스 예외 =====

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.error("Business exception: {} ({})", ex.getMessage(), ex.getErrorCode().getCode());
        ErrorCode errorCode = ex.getErrorCode();
        return createErrorResponse(errorCode, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ErrorCode.USER_NOT_FOUND.getCode(),
                ex.getMessage(),
                LocalDateTime.now());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        log.error("Unauthorized: {}", ex.getMessage());
        return createErrorResponse(ex.getErrorCode(), ex.getMessage());
    }

    // ===== 인증/인가 예외 (Spring Security) =====

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.ACCESS_DENIED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.error("Authentication failed: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.AUTHENTICATION_ERROR);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Bad credentials: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.INVALID_CREDENTIALS);
    }

    // ===== 요청 유효성 검증 예외 =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        return createErrorResponse(ErrorCode.INVALID_INPUT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        ValidationErrorResponse errorResponse = new ValidationErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                errors);

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    // ===== 런타임 예외 (예상치 못한 오류) =====

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected runtime error: ", ex);
        return createErrorResponse(ErrorCode.INTERNAL_ERROR);
    }

    // ===== 최종 방어선: 모든 예외 처리 =====
    // Checked Exception (IOException, SQLException 등) 포함
    // 어떤 상황에서도 HTML 대신 JSON 응답 보장

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception ex) {
        log.error("Unhandled exception caught (General): ", ex);
        return createErrorResponse(ErrorCode.INTERNAL_ERROR);
    }

    // ===== Helper Methods =====

    private ResponseEntity<ErrorResponse> createErrorResponse(ErrorCode errorCode) {
        return createErrorResponse(errorCode, errorCode.getMessage());
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(ErrorCode errorCode, String message) {
        ErrorResponse error = new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                message,
                LocalDateTime.now());
        return new ResponseEntity<>(error, errorCode.getHttpStatus());
    }

    // ===== Response Records =====

    record ErrorResponse(int status, String code, String message, LocalDateTime timestamp) {
    }

    record ValidationErrorResponse(int status, String code, String message, LocalDateTime timestamp,
            Map<String, String> errors) {
    }
}
