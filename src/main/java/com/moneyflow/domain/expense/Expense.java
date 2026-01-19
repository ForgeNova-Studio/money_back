package com.moneyflow.domain.expense;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 소속 장부 (여행, 생활비 등)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_book_id")
    private com.moneyflow.domain.accountbook.AccountBook accountBook;

    /**
     * 지출 출처 (개인 vs 공금)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "funding_source", length = 20)
    @Builder.Default
    private com.moneyflow.domain.accountbook.FundingSource fundingSource = com.moneyflow.domain.accountbook.FundingSource.PERSONAL;

    @Column(nullable = false, precision = 12, scale = 2)
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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
