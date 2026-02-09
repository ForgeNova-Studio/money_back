package com.moneyflow.domain.accountbook;

import com.moneyflow.domain.couple.Couple;
import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 가계부/장부 엔티티
 *
 * 커플 생활비, 여행 경비 등 다양한 유형의 장부를 관리합니다.
 * - 커플 장부: couple_id로 연결
 * - 여행/프로젝트: 별도 멤버 관리
 */
@Entity
@Table(name = "account_books", indexes = {
        @Index(name = "idx_account_books_couple", columnList = "couple_id"),
        @Index(name = "idx_account_books_created_by", columnList = "created_by"),
        @Index(name = "idx_account_books_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_book_id")
    private UUID accountBookId;

    /**
     * 장부 이름 (예: "2025 생활비", "일본 여행")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 장부 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "book_type", nullable = false, length = 20)
    private BookType bookType;

    /**
     * 커플 장부인 경우 커플 연결
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "couple_id")
    private Couple couple;

    /**
     * 정산용 참여 인원수
     */
    @Builder.Default
    @Column(name = "member_count")
    private Integer memberCount = 2;

    /**
     * 장부 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 시작일 (여행 시작일 등)
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * 종료일
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * 활성 상태
     */
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * 초기 잔액 (장부 시작 시 보유 금액)
     * 총자산 계산: 초기잔액 + 총수입 - 총지출
     */
    @Builder.Default
    @Column(name = "initial_balance", precision = 15, scale = 2)
    private java.math.BigDecimal initialBalance = java.math.BigDecimal.ZERO;

    /**
     * 장부 생성자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User createdBy;

    /**
     * 장부 멤버 목록
     */
    @OneToMany(mappedBy = "accountBook", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccountBookMember> members = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // === 비즈니스 메서드 ===

    /**
     * 커플 장부인지 확인
     */
    public boolean isCoupleBook() {
        return couple != null;
    }

    /**
     * 사용자가 이 장부의 멤버인지 확인
     */
    public boolean isMember(UUID userId) {
        return members.stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
    }

    /**
     * 사용자가 이 장부의 Owner인지 확인
     */
    public boolean isOwner(UUID userId) {
        return members.stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId)
                        && m.getRole() == MemberRole.OWNER);
    }

    /**
     * 멤버 추가
     */
    public void addMember(User user, MemberRole role) {
        AccountBookMember member = AccountBookMember.builder()
                .id(new AccountBookMemberId(this.accountBookId, user.getUserId()))
                .accountBook(this)
                .user(user)
                .role(role)
                .joinedAt(LocalDateTime.now())
                .build();
        this.members.add(member);
    }

    /**
     * 장부 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
}
