package com.moneyflow.dto.request;

/**
 * 요청 DTO 공통 검증 상수
 *
 * @Pattern 등 Jakarta Validation 어노테이션의 속성값으로 사용하려면
 * compile-time constant(static final String)이어야 합니다.
 */
public final class ValidationConstants {

    private ValidationConstants() {}

    public static final String PASSWORD_REGEXP =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    public static final String PASSWORD_MESSAGE =
            "비밀번호는 대문자, 소문자, 숫자, 특수문자(@$!%*?&)를 각각 최소 1개 이상 포함해야 합니다";
}
