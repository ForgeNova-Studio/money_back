package com.moneyflow.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 인증 정보 엔티티
 * 
 * Users 테이블과 1:N 관계로, 한 사용자가 여러 로그인 수단을 가질 수 있습니다.
 * - EMAIL: passwordHash 필드에 암호화된 비밀번호 저장
 * - GOOGLE/KAKAO/NAVER: providerId 필드에 소셜 로그인 고유 ID 저장
 */
@Entity
@Table(name = "user_auths", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "provider", "provider_id" }),
        @UniqueConstraint(columnNames = { "user_id", "provider" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "auth_id")
    private UUID authId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId; // 소셜 로그인 제공자의 사용자 고유 ID (EMAIL이면 null)

    @Column(name = "password_hash")
    private String passwordHash; // EMAIL일 때만 값 존재, 소셜이면 null

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
