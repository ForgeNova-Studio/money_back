package com.moneyflow.domain.couple;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 커플 엔티티
 *
 * 두 사용자가 가계부를 공유하기 위한 커플 그룹 정보를 저장합니다.
 */
@Entity
@Table(name = "couples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Couple {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "couple_id", updatable = false, nullable = false)
    private UUID coupleId;

    /**
     * 초대를 생성한 사용자 (User 1)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    /**
     * 초대를 수락한 사용자 (User 2)
     * NULL이면 아직 초대가 수락되지 않은 상태
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id")
    private User user2;

    /**
     * 초대 코드 (6자리 영숫자)
     * 예: "A3B9C2"
     */
    @Column(name = "invite_code", length = 10, unique = true)
    private String inviteCode;

    /**
     * 초대 코드 만료 시간
     * 생성 후 7일 후 만료
     */
    @Column(name = "code_expires_at")
    private LocalDateTime codeExpiresAt;

    /**
     * 커플 연동 완료 시간
     * user2가 초대를 수락한 시간
     */
    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        // 초대 코드 만료 시간 설정 (7일)
        if (this.codeExpiresAt == null && this.inviteCode != null) {
            this.codeExpiresAt = LocalDateTime.now().plusDays(7);
        }
    }

    /**
     * 커플 연동 완료 여부
     */
    public boolean isLinked() {
        return user2 != null && linkedAt != null;
    }

    /**
     * 초대 코드 만료 여부
     */
    public boolean isCodeExpired() {
        return codeExpiresAt != null && LocalDateTime.now().isAfter(codeExpiresAt);
    }

    /**
     * 특정 사용자가 이 커플의 멤버인지 확인
     */
    public boolean isMember(UUID userId) {
        if (user1 != null && user1.getUserId().equals(userId)) {
            return true;
        }
        if (user2 != null && user2.getUserId().equals(userId)) {
            return true;
        }
        return false;
    }
}
