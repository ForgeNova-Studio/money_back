package com.moneyflow.service;

import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.dto.request.SocialLoginRequest;
import com.moneyflow.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialLoginServiceTest {

    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private NaverOAuthService naverOAuthService;

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @Mock
    private SocialLoginPersistenceService socialLoginPersistenceService;

    @Mock
    private DevelopmentOnlyAccessGuard developmentOnlyAccessGuard;

    @InjectMocks
    private SocialLoginService socialLoginService;

    @Test
    @DisplayName("카카오 로그인: 카카오 API 이메일이 없으면 클라이언트 이메일을 무시하고 실패한다")
    void login_kakaoWithoutEmail_ignoresClientEmailAndFails() {
        SocialLoginRequest request = new SocialLoginRequest(
                AuthProvider.KAKAO,
                "kakao-token",
                "클라이언트닉네임",
                "attacker@example.com");

        when(kakaoOAuthService.verifyAccessToken("kakao-token"))
                .thenReturn(new KakaoOAuthService.KakaoUserInfo("kakao-123", "", "카카오닉네임", null));

        assertThatThrownBy(() -> socialLoginService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이메일 정보를 가져올 수 없습니다");

        verify(socialLoginPersistenceService, never()).loginOrRegister(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }
}
