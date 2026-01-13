package com.moneyflow.domain.accountbook;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 장부 멤버 엔티티 (N:N 매핑)
 *
 * 장부와 사용자 간의 다대다 관계를 관리합니다.
 */
@Entity
@Table(name = "account_book_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBookMember {

    @EmbeddedId
    private AccountBookMemberId id;

    /**
     * 소속 장부
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("accountBookId")
    @JoinColumn(name = "account_book_id")
    private AccountBook accountBook;

    /**
     * 멤버 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 멤버 역할 (OWNER, MEMBER)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    /**
     * 참여 시간
     */
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}
