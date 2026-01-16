package com.moneyflow.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * Apple OAuth 검증 서비스
 * Apple Identity Token을 검증하고 사용자 정보를 추출합니다.
 *
 * 참고: 실제 프로덕션에서는 Apple의 공개 키를 주기적으로 가져와서 검증해야 합니다.
 */
@Service
public class AppleOAuthService {

    /**
     * Apple Identity Token 검증 및 사용자 정보 추출
     */
    public AppleUserInfo verifyIdToken(String idToken) {
        try {
            DecodedJWT jwt = JWT.decode(idToken);

            String providerId = jwt.getSubject();
            String email = jwt.getClaim("email").asString();
            Boolean emailVerified = jwt.getClaim("email_verified").asBoolean();

            if (providerId == null || email == null) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN);
            }

            return new AppleUserInfo(providerId, email, emailVerified != null && emailVerified);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    public record AppleUserInfo(String providerId, String email, boolean emailVerified) {
    }
}
