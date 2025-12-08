package com.moneyflow.domain.token;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh Token 엔티티
 *
 * JWT Refresh Token을 DB에 저장하여 다음 기능을 지원합니다:
 * - 로그아웃: 토큰 무효화
 * - 보안 강화: 탈취된 토큰 강제 무효화
 * - Rotation 정책: Refresh 시 기존 토큰 무효화 & 새 토큰 발급
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 토큰 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Refresh Token의 SHA-256 해시값
     *
     * 보안을 위해 실제 토큰이 아닌 해시값만 저장합니다.
     * DB 유출 시에도 실제 토큰을 복원할 수 없습니다.
     */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    /**
     * 토큰 만료 시간
     *
     * JWT의 exp claim과 동일한 값을 저장합니다.
     * DB에서도 만료 여부를 확인할 수 있습니다.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 토큰 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 토큰 무효화 여부
     *
     * true: 로그아웃되었거나 Rotation으로 무효화됨
     * false: 사용 가능한 토큰
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 토큰이 유효한지 확인 (무효화되지 않고 만료되지 않음)
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * 토큰 무효화
     */
    public void revoke() {
        this.revoked = true;
    }
}
