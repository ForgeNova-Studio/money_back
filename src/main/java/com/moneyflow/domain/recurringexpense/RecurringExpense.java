package com.moneyflow.domain.recurringexpense;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 고정비 및 구독료 엔티티
 * 월세, 보험료, 구독료 등 반복적인 지출을 관리합니다.
 */
@Entity
@Table(name = "recurring_expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "recurring_expense_id")
    private UUID recurringExpenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 소속 장부 (여행, 생활비 등)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_book_id")
    private com.moneyflow.domain.accountbook.AccountBook accountBook;

    // 기본 정보
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 반복 정보
    @Enumerated(EnumType.STRING)
    @Column(name = "recurring_type", nullable = false, length = 20)
    private RecurringType recurringType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "next_payment_date", nullable = false)
    private LocalDate nextPaymentDate;

    // 구독료 관련
    @Column(name = "is_subscription")
    private Boolean isSubscription = false;

    @Column(name = "subscription_provider", length = 100)
    private String subscriptionProvider;

    // 알림 및 변동 감지
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Column(name = "last_amount", precision = 15, scale = 2)
    private BigDecimal lastAmount;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    // 자동 탐지 관련
    @Column(name = "auto_detected")
    private Boolean autoDetected = false;

    @Column(name = "detection_confidence", precision = 3, scale = 2)
    private BigDecimal detectionConfidence;

    // 메타 정보
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 다음 결제 날짜 계산
     * 반복 주기에 따라 다음 결제 날짜를 자동으로 계산합니다.
     */
    public void calculateNextPaymentDate() {
        LocalDate today = LocalDate.now();
        LocalDate calculatedDate = nextPaymentDate;

        // 다음 결제일이 과거인 경우만 재계산
        while (calculatedDate.isBefore(today) || calculatedDate.isEqual(today)) {
            switch (recurringType) {
                case MONTHLY:
                    calculatedDate = calculatedDate.plusMonths(1);
                    break;
                case YEARLY:
                    calculatedDate = calculatedDate.plusYears(1);
                    break;
                case WEEKLY:
                    calculatedDate = calculatedDate.plusWeeks(1);
                    break;
            }
        }

        this.nextPaymentDate = calculatedDate;
    }

    /**
     * 금액 변동 감지
     * 
     * @return 금액이 변경되었으면 true
     */
    public boolean hasAmountChanged() {
        if (lastAmount == null) {
            return false;
        }
        return lastAmount.compareTo(amount) != 0;
    }

    /**
     * 종료 여부 확인
     * 
     * @return 종료일이 지났으면 true
     */
    public boolean isExpired() {
        if (endDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(endDate);
    }
}
