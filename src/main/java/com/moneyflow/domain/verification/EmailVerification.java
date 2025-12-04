package com.moneyflow.domain.verification;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이메일 인증 엔티티
 * 회원가입 또는 비밀번호 재설정 시 이메일 인증을 위한 코드 저장
 */
@Entity
@Table(name = "email_verifications", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_verification_code", columnList = "verification_code"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * 이메일 주소
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * 6자리 인증 코드
     */
    @Column(name = "verification_code", nullable = false, length = 6)
    private String verificationCode;

    /**
     * 인증 유형 (SIGNUP: 회원가입, PASSWORD_RESET: 비밀번호 재설정)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_type", nullable = false, length = 20)
    private VerificationType verificationType;

    /**
     * 인증 완료 여부
     */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    /**
     * 만료 시간 (생성 시간 + 10분)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 인증 완료 시간
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            // 기본 만료 시간: 10분
            expiresAt = createdAt.plusMinutes(10);
        }
    }

    /**
     * 인증 코드 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 인증 완료 처리
     */
    public void markAsVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * 회원가입 가능 시간 만료 여부 확인
     * 인증 완료 후 5분 이내에만 회원가입 가능
     */
    public boolean isExpiredForRegistration() {
        if (verifiedAt == null) {
            return true; // 인증 완료되지 않음
        }
        return LocalDateTime.now().isAfter(verifiedAt.plusMinutes(5));
    }

    /**
     * 인증 유형 열거형
     */
    public enum VerificationType {
        SIGNUP,          // 회원가입
        PASSWORD_RESET   // 비밀번호 재설정
    }
}
