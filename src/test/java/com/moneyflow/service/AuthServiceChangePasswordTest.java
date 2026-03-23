package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBookService;
import com.moneyflow.domain.terms.TermsService;
import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.UserAuth;
import com.moneyflow.domain.user.UserAuthRepository;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceChangePasswordTest {

    @Mock private UserRepository userRepository;
    @Mock private UserAuthRepository userAuthRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AccountBookService accountBookService;
    @Mock private TermsService termsService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private TokenService tokenService;
    @Mock private DevelopmentOnlyAccessGuard developmentOnlyAccessGuard;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("비밀번호 변경: 정상 케이스 — 새 비밀번호 해시가 저장된다")
    void changePassword_success() {
        UUID userId = UUID.randomUUID();
        String currentRaw = "Current123!";
        String newRaw = "NewPass456!";
        String encodedNew = "$2a$10$newHashedValue";

        UserAuth emailAuth = UserAuth.builder()
                .user(null)
                .provider(AuthProvider.EMAIL)
                .passwordHash("$2a$10$currentHashedValue")
                .build();

        when(userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL))
                .thenReturn(Optional.of(emailAuth));
        when(passwordEncoder.matches(currentRaw, emailAuth.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(newRaw)).thenReturn(encodedNew);

        authService.changePassword(userId, currentRaw, newRaw);

        assertThat(emailAuth.getPasswordHash()).isEqualTo(encodedNew);
        verify(userAuthRepository).save(emailAuth);
    }

    @Test
    @DisplayName("비밀번호 변경: 소셜 전용 계정 — ACCESS_DENIED 예외(403)")
    void changePassword_socialOnlyAccount_throwsAccessDenied() {
        UUID userId = UUID.randomUUID();

        when(userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(userId, "any", "NewPass456!"))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(ex -> assertThat(((UnauthorizedException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ACCESS_DENIED));

        verify(userAuthRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호 불일치 — INVALID_CREDENTIALS 예외(401)")
    void changePassword_wrongCurrentPassword_throwsInvalidCredentials() {
        UUID userId = UUID.randomUUID();
        String wrongPassword = "WrongPass123!";

        UserAuth emailAuth = UserAuth.builder()
                .user(null)
                .provider(AuthProvider.EMAIL)
                .passwordHash("$2a$10$correctHash")
                .build();

        when(userAuthRepository.findByUserUserIdAndProvider(userId, AuthProvider.EMAIL))
                .thenReturn(Optional.of(emailAuth));
        when(passwordEncoder.matches(wrongPassword, emailAuth.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(userId, wrongPassword, "NewPass456!"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(userAuthRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
