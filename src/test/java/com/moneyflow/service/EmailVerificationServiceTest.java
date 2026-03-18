package com.moneyflow.service;

import com.moneyflow.domain.user.UserAuthRepository;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.SendCodeRequest;
import com.moneyflow.domain.verification.EmailVerification;
import com.moneyflow.domain.verification.EmailVerificationRepository;
import com.moneyflow.dto.request.VerifyCodeRequest;
import com.moneyflow.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("회원가입 인증 코드 검증: 잘못된 코드면 시도 횟수를 증가시킨다")
    void verifySignupCode_incrementsAttemptCountOnFailure() {
        EmailVerification verification = verification(0);
        when(emailVerificationRepository.findFirstByEmailAndVerificationTypeAndVerifiedFalseOrderByCreatedAtDesc(
                "user@test.com",
                EmailVerification.VerificationType.SIGNUP))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifySignupCode(
                new VerifyCodeRequest("user@test.com", "000000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("인증 코드가 일치하지 않습니다");

        assertThat(verification.getAttemptCount()).isEqualTo(1);
        verify(emailVerificationRepository).save(verification);
        verify(emailVerificationRepository, never()).delete(verification);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증 코드 검증: 최대 시도 횟수 도달 시 코드를 폐기한다")
    void verifyPasswordResetCode_deletesVerificationWhenAttemptsExceeded() {
        EmailVerification verification = verification(4);
        verification.setVerificationType(EmailVerification.VerificationType.PASSWORD_RESET);

        when(emailVerificationRepository.findFirstByEmailAndVerificationTypeAndVerifiedFalseOrderByCreatedAtDesc(
                "user@test.com",
                EmailVerification.VerificationType.PASSWORD_RESET))
                .thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifyPasswordResetCode(
                new VerifyCodeRequest("user@test.com", "000000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("시도 횟수를 초과");

        assertThat(verification.getAttemptCount()).isEqualTo(5);
        verify(emailVerificationRepository).delete(verification);
        verify(emailVerificationRepository, never()).save(verification);
    }

    @Test
    @DisplayName("회원가입 인증 코드 발송: 동일 이메일 재요청은 1분 쿨다운을 적용한다")
    void sendSignupCode_appliesCooldownPerEmail() {
        EmailVerification latest = verification(0);
        latest.setCreatedAt(LocalDateTime.now().minusSeconds(30));

        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(emailVerificationRepository.findFirstByEmailAndVerificationTypeOrderByCreatedAtDesc(
                "user@test.com",
                EmailVerification.VerificationType.SIGNUP))
                .thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> emailVerificationService.sendSignupCode(
                new SendCodeRequest("user@test.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1분 후");

        verify(emailVerificationRepository, never()).deleteByEmailAndVerifiedFalse("user@test.com");
        verify(emailService, never()).sendSignupVerificationEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private EmailVerification verification(int attemptCount) {
        return EmailVerification.builder()
                .email("user@test.com")
                .verificationCode("123456")
                .verificationType(EmailVerification.VerificationType.SIGNUP)
                .attemptCount(attemptCount)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }
}
