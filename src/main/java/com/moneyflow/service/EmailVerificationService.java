package com.moneyflow.service;

import com.moneyflow.domain.user.AuthProvider;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserAuth;
import com.moneyflow.domain.user.UserAuthRepository;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.domain.verification.EmailVerification;
import com.moneyflow.domain.verification.EmailVerificationRepository;
import com.moneyflow.dto.request.ChangePasswordRequest;
import com.moneyflow.dto.request.SendCodeRequest;
import com.moneyflow.dto.request.VerifyCodeRequest;
import com.moneyflow.dto.response.VerificationResponse;
import com.moneyflow.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void consumeVerifiedSignup(String email) {
        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedTrueOrderByVerifiedAtDesc(
                        email,
                        EmailVerification.VerificationType.SIGNUP)
                .orElseThrow(() -> new BusinessException("이메일 인증을 먼저 완료해주세요"));

        if (verification.isExpiredForRegistration()) {
            throw new BusinessException("인증 시간이 만료되었습니다. 다시 인증해주세요");
        }

        emailVerificationRepository.delete(verification);
        log.info("회원가입 인증 확인 완료: {}", email);
    }

    @Transactional
    public VerificationResponse sendSignupCode(SendCodeRequest request) {
        String email = request.getEmail();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("이미 가입된 이메일입니다");
        }

        validateSendRateLimit(email, EmailVerification.VerificationType.SIGNUP);
        emailVerificationRepository.deleteByEmailAndVerifiedFalse(email);

        String code = emailService.generateVerificationCode();
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .verificationType(EmailVerification.VerificationType.SIGNUP)
                .build();
        emailVerificationRepository.save(verification);

        emailService.sendSignupVerificationEmail(email, code);

        log.info("회원가입 인증 코드 발송: {}", email);
        return VerificationResponse.success("인증 코드가 발송되었습니다. 이메일을 확인해주세요.");
    }

    @Transactional
    public VerificationResponse verifySignupCode(VerifyCodeRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedFalseOrderByCreatedAtDesc(
                        email,
                        EmailVerification.VerificationType.SIGNUP)
                .orElseThrow(() -> new BusinessException("인증 코드를 찾을 수 없습니다"));

        if (verification.isExpired()) {
            throw new BusinessException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        validateVerificationCode(verification, code, email);

        verification.markAsVerified();
        emailVerificationRepository.save(verification);

        log.info("회원가입 인증 코드 검증 완료: {}", email);
        return VerificationResponse.success("인증이 완료되었습니다");
    }

    @Transactional
    public VerificationResponse sendPasswordResetCode(SendCodeRequest request) {
        String email = request.getEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("가입되지 않은 이메일입니다"));

        boolean hasEmailAuth = userAuthRepository.findByUserUserIdAndProvider(user.getUserId(), AuthProvider.EMAIL)
                .isPresent();
        if (!hasEmailAuth) {
            throw new BusinessException("소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다");
        }

        validateSendRateLimit(email, EmailVerification.VerificationType.PASSWORD_RESET);
        emailVerificationRepository.deleteByEmailAndVerifiedFalse(email);

        String code = emailService.generateVerificationCode();
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .verificationType(EmailVerification.VerificationType.PASSWORD_RESET)
                .build();
        emailVerificationRepository.save(verification);

        emailService.sendPasswordResetEmail(email, code);

        log.info("비밀번호 재설정 인증 코드 발송: {}", email);
        return VerificationResponse.success("인증 코드가 발송되었습니다. 이메일을 확인해주세요.");
    }

    @Transactional
    public VerificationResponse verifyPasswordResetCode(VerifyCodeRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedFalseOrderByCreatedAtDesc(
                        email,
                        EmailVerification.VerificationType.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException("인증 코드를 찾을 수 없습니다"));

        if (verification.isExpired()) {
            throw new BusinessException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        validateVerificationCode(verification, code, email);

        verification.markAsVerified();
        emailVerificationRepository.save(verification);

        log.info("비밀번호 재설정 인증 코드 검증 완료: {}", email);
        return VerificationResponse.success("인증이 완료되었습니다. 새 비밀번호를 설정해주세요.");
    }

    @Transactional
    public VerificationResponse resetPassword(ChangePasswordRequest request) {
        String email = request.getEmail();
        String newPassword = request.getNewPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("가입되지 않은 이메일입니다"));

        UserAuth emailAuth = userAuthRepository.findByUserUserIdAndProvider(user.getUserId(), AuthProvider.EMAIL)
                .orElseThrow(() -> new BusinessException("소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다"));

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndVerificationTypeAndVerifiedTrueOrderByVerifiedAtDesc(
                        email,
                        EmailVerification.VerificationType.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException("인증을 먼저 완료해주세요"));

        if (verification.isExpiredForRegistration()) {
            throw new BusinessException("인증 시간이 만료되었습니다. 다시 인증해주세요.");
        }

        emailAuth.setPasswordHash(passwordEncoder.encode(newPassword));
        // 비밀번호 변경을 먼저 flush해서 DB 반영 가능 여부를 확인한 뒤 인증 기록을 정리한다.
        // 이후 단계에서 예외가 발생하면 @Transactional에 의해 두 변경 모두 롤백된다.
        userAuthRepository.saveAndFlush(emailAuth);
        emailVerificationRepository.delete(verification);

        log.info("비밀번호 재설정 완료: {}", email);
        return VerificationResponse.success("비밀번호가 재설정되었습니다");
    }

    private void validateVerificationCode(EmailVerification verification, String code, String email) {
        if (verification.hasExceededAttempts(MAX_VERIFICATION_ATTEMPTS)) {
            emailVerificationRepository.delete(verification);
            log.warn("인증 시도 횟수 초과로 코드 폐기: email={}, type={}", email, verification.getVerificationType());
            throw new BusinessException("인증 시도 횟수를 초과했습니다. 코드를 다시 요청해주세요.");
        }

        if (verification.getVerificationCode().equals(code)) {
            return;
        }

        verification.incrementAttemptCount();
        if (verification.hasExceededAttempts(MAX_VERIFICATION_ATTEMPTS)) {
            emailVerificationRepository.delete(verification);
            log.warn("인증 시도 횟수 초과로 코드 폐기: email={}, type={}", email, verification.getVerificationType());
            throw new BusinessException("인증 시도 횟수를 초과했습니다. 코드를 다시 요청해주세요.");
        }

        emailVerificationRepository.save(verification);
        throw new BusinessException("인증 코드가 일치하지 않습니다");
    }

    private void validateSendRateLimit(String email, EmailVerification.VerificationType verificationType) {
        emailVerificationRepository.findFirstByEmailAndVerificationTypeOrderByCreatedAtDesc(email, verificationType)
                .ifPresent(latest -> {
                    LocalDateTime availableAt = latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
                    if (LocalDateTime.now().isBefore(availableAt)) {
                        throw new BusinessException("인증 코드는 1분 후에 다시 요청할 수 있습니다.");
                    }
                });
    }
}
