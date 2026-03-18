package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBookService;
import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.dto.request.SocialLoginRequest;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialLoginService {

    private final GoogleOAuthService googleOAuthService;
    private final NaverOAuthService naverOAuthService;
    private final KakaoOAuthService kakaoOAuthService;
    private final SocialLoginPersistenceService socialLoginPersistenceService;
    private final DevelopmentOnlyAccessGuard developmentOnlyAccessGuard;

    public LoginResponse login(SocialLoginRequest request) {
        try {
            SocialProfile socialProfile = resolveSocialProfile(request);
            String email = normalizeEmail(socialProfile.email());
            if (email == null) {
                throw new BusinessException(
                        "소셜 계정에서 이메일 정보를 가져올 수 없습니다. 이메일 제공 동의 후 다시 시도해주세요.",
                        ErrorCode.INVALID_INPUT);
            }

            String name = normalizeName(socialProfile.name());
            return socialLoginPersistenceService.loginOrRegister(
                    request.getProvider(),
                    email,
                    socialProfile.providerId(),
                    name);
        } catch (GeneralSecurityException | IOException e) {
            log.error("소셜 로그인 토큰 검증 실패", e);
            throw new BusinessException("유효하지 않은 소셜 로그인 토큰입니다");
        } catch (IllegalArgumentException e) {
            log.error("소셜 로그인 토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException("유효하지 않은 소셜 로그인 토큰입니다");
        }
    }

    public LoginResponse mockLogin(SocialLoginRequest request) {
        developmentOnlyAccessGuard.validate("Mock 소셜 로그인");
        log.warn("⚠️ Mock 소셜 로그인 사용 중 - 개발/테스트 전용");

        String email = "mock_" + request.getProvider().name().toLowerCase() + "_" +
                System.currentTimeMillis() + "@test.com";
        String providerId = "MOCK_" + System.currentTimeMillis();
        String name = request.getNickname() != null ? request.getNickname() : "Mock User";

        return socialLoginPersistenceService.mockLogin(request.getProvider(), email, providerId, name);
    }

    private SocialProfile resolveSocialProfile(SocialLoginRequest request)
            throws GeneralSecurityException, IOException {
        String name = request.getNickname();

        if (request.getProvider() == AuthProvider.GOOGLE) {
            GoogleOAuthService.GoogleUserInfo userInfo = googleOAuthService.verifyIdToken(request.getIdToken());
            return new SocialProfile(userInfo.email(), userInfo.providerId(), name == null ? userInfo.name() : name);
        }

        if (request.getProvider() == AuthProvider.NAVER) {
            NaverOAuthService.NaverUserInfo userInfo = naverOAuthService.verifyAccessToken(request.getIdToken());
            return new SocialProfile(userInfo.email(), userInfo.providerId(), name == null ? userInfo.name() : name);
        }

        if (request.getProvider() == AuthProvider.KAKAO) {
            KakaoOAuthService.KakaoUserInfo userInfo = kakaoOAuthService.verifyAccessToken(request.getIdToken());
            return new SocialProfile(
                    userInfo.email(),
                    userInfo.providerId(),
                    name == null ? userInfo.nickname() : name);
        }

        throw new BusinessException("지원하지 않는 로그인 제공자입니다");
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String normalized = email.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record SocialProfile(String email, String providerId, String name) {
    }
}
