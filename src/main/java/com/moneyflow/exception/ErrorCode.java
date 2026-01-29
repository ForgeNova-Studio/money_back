package com.moneyflow.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 *
 * 형식: [도메인]_[에러유형]
 * HTTP 상태 코드와 사용자 메시지를 함께 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== 공통 (COMMON) =====
    INVALID_INPUT("C001", "입력값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("C002", "입력값 검증에 실패했습니다", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("C003", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== 인증 (AUTH) =====
    INVALID_CREDENTIALS("A001", "이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("A002", "인증이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("A003", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    EMAIL_ALREADY_EXISTS("A004", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),
    INVALID_OAUTH_TOKEN("A005", "유효하지 않은 소셜 로그인 토큰입니다", HttpStatus.UNAUTHORIZED),
    OAUTH_API_ERROR("A006", "소셜 로그인 서비스 오류가 발생했습니다", HttpStatus.BAD_GATEWAY),
    AUTHENTICATION_ERROR("A007", "인증에 실패했습니다", HttpStatus.UNAUTHORIZED),

    // ===== 사용자 (USER) =====
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // ===== 장부 (ACCOUNT_BOOK) =====
    ACCOUNT_BOOK_NOT_FOUND("AB001", "장부를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ACCOUNT_BOOK_REQUIRED("AB002", "장부 ID는 필수입니다", HttpStatus.BAD_REQUEST),
    ACCOUNT_BOOK_ACCESS_DENIED("AB003", "해당 장부에 접근할 권한이 없습니다", HttpStatus.FORBIDDEN),

    // ===== 지출 (EXPENSE) =====
    EXPENSE_NOT_FOUND("E001", "지출 내역을 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // ===== 수입 (INCOME) =====
    INCOME_NOT_FOUND("I001", "수입 내역을 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // ===== 고정비 (RECURRING) =====
    RECURRING_EXPENSE_NOT_FOUND("R001", "고정비를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // ===== 자산 (ASSET) =====
    ASSET_NOT_FOUND("AS001", "자산을 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // ===== 커플 (COUPLE) =====
    COUPLE_NOT_FOUND("CP001", "커플 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ALREADY_COUPLED("CP002", "이미 커플이 연결되어 있습니다", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
