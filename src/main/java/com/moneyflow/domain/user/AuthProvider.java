package com.moneyflow.domain.user;

/**
 * 인증 제공자 Enum
 * 사용자가 로그인한 방식을 나타냅니다.
 */
public enum AuthProvider {
    EMAIL,    // 이메일/비밀번호 로그인
    GOOGLE,   // Google OAuth 로그인
    APPLE,    // Apple Sign In
    NAVER,    // Naver OAuth 로그인
    KAKAO     // Kakao OAuth 로그인
}
