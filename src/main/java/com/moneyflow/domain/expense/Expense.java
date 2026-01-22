package com.moneyflow.domain.expense;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.FundingSource;
import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "expense_id")
    private UUID expenseId;

    /**
     * 지출 등록자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 실제 결제한 사람 (SHARED_POOL일 때 사용)
     * - PERSONAL: user와 동일 (또는 null)
     * - SHARED_POOL: 실제 결제한 사람 (정산 계산용)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id")
    private User paidBy;

    /**
     * 소속 장부 (여행, 생활비 등)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_book_id")
    private AccountBook accountBook;

    /**
     * 원본 장부 ID (장부 이동 시 이력 추적용)
     * - 여행 장부 → 개인 장부로 이동 시 원본 여행 장부 ID 저장
     */
    @Column(name = "original_account_book_id")
    private UUID originalAccountBookId;

    /**
     * 지출 출처 (개인 vs 공금)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "funding_source", length = 20)
    @Builder.Default
    private FundingSource fundingSource = FundingSource.PERSONAL;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 255)
    private String merchant;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_auto_categorized")
    private Boolean isAutoCategorized = false;

    /**
     * 연결된 고정비 결제 ID (매칭 확정 시 설정)
     */
    @Column(name = "linked_payment_id")
    private UUID linkedPaymentId;

    /**
     * 지출 참여자 목록 (SHARED_POOL일 때 N빵 대상자)
     */
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseParticipant> participants = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 비즈니스 메서드 =====

    /**
     * 공용 지출인지 확인
     */
    public boolean isSharedExpense() {
        return fundingSource == FundingSource.SHARED_POOL;
    }

    /**
     * 개인 지출인지 확인
     */
    public boolean isPersonalExpense() {
        return fundingSource == FundingSource.PERSONAL;
    }

    /**
     * 이동된 지출인지 확인
     */
    public boolean isTransferred() {
        return originalAccountBookId != null;
    }
}
