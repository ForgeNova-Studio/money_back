package com.moneyflow.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Apple OAuth 검증 서비스
 * Apple Identity Token을 검증하고 사용자 정보를 추출합니다.
 *
 * 참고: 실제 프로덕션에서는 Apple의 공개 키를 주기적으로 가져와서 검증해야 합니다.
 * 현재는 간단한 디코딩 방식으로 구현했습니다.
 */
@Service
public class AppleOAuthService {

    /**
     * Apple Identity Token 검증 및 사용자 정보 추출
     *
     * 주의: 이 구현은 개발 단계용입니다.
     * 프로덕션 환경에서는 Apple의 공개 키를 이용한 완전한 검증이 필요합니다.
     *
     * @param idToken Apple Identity Token
     * @return 사용자 정보 (이메일, providerId)
     * @throws IllegalArgumentException 유효하지 않은 토큰
     */
    public AppleUserInfo verifyIdToken(String idToken) {
        try {
            // JWT 디코딩 (검증 없이 클레임 추출)
            // 프로덕션에서는 Apple의 공개 키로 서명 검증 필요
            DecodedJWT jwt = JWT.decode(idToken);

            // 클레임에서 사용자 정보 추출
            String providerId = jwt.getSubject();  // Apple User ID
            String email = jwt.getClaim("email").asString();

            // Apple은 이메일 검증 여부 제공
            Boolean emailVerified = jwt.getClaim("email_verified").asBoolean();

            if (providerId == null || email == null) {
                throw new IllegalArgumentException("Invalid Apple ID token: missing required claims");
            }

            return new AppleUserInfo(providerId, email, emailVerified != null && emailVerified);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Apple ID token: " + e.getMessage(), e);
        }
    }

    /**
     * Apple 사용자 정보
     */
    public record AppleUserInfo(
            String providerId,
            String email,
            boolean emailVerified
    ) {}
}
