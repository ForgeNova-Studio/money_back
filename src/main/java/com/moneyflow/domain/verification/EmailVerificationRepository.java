package com.moneyflow.domain.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    /**
     * 이메일과 인증 코드로 인증 정보 조회
     */
    Optional<EmailVerification> findByEmailAndVerificationCode(String email, String verificationCode);

    /**
     * 이메일과 인증 유형으로 미완료 인증 정보 조회 (최신순)
     */
    Optional<EmailVerification> findFirstByEmailAndVerificationTypeAndVerifiedFalseOrderByCreatedAtDesc(
            String email, EmailVerification.VerificationType verificationType);

    /**
     * 만료된 인증 코드 일괄 삭제 (배치 작업용)
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * 특정 이메일의 모든 미완료 인증 정보 삭제
     */
    void deleteByEmailAndVerifiedFalse(String email);

    /**
     * 인증 완료된 정보 조회
     */
    Optional<EmailVerification> findByEmailAndVerificationTypeAndVerifiedTrue(
            String email, EmailVerification.VerificationType verificationType);
}
