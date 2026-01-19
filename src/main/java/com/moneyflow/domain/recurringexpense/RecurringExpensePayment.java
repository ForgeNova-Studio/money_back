package com.moneyflow.domain.recurringexpense;

import com.moneyflow.domain.expense.Expense;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 월별 고정비 결제 이력 엔티티
 *
 * 각 고정비에 대해 월별로 하나의 레코드가 생성됩니다.
 * - PENDING: 아직 실제 지출과 매칭되지 않음 (예상 금액으로 표시)
 * - CONFIRMED: 실제 지출과 매칭되어 확정됨
 * - SKIPPED: 이번 달은 건너뜀
 */
@Entity
@Table(name = "recurring_expense_payments", uniqueConstraints = @UniqueConstraint(columnNames = {
        "recurring_expense_id", "period_year", "period_month" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringExpensePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID paymentId;

    /**
     * 연결된 고정비
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_expense_id", nullable = false)
    private RecurringExpense recurringExpense;

    /**
     * 연결된 실제 지출 (CONFIRMED 시에만 존재)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    /**
     * 결제 기간 (년)
     */
    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    /**
     * 결제 기간 (월)
     */
    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    /**
     * 예상 금액 (고정비 등록 당시 금액)
     */
    @Column(name = "expected_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal expectedAmount;

    /**
     * 실제 금액 (CONFIRMED 시 설정)
     */
    @Column(name = "actual_amount", precision = 15, scale = 2)
    private BigDecimal actualAmount;

    /**
     * 예상 결제일
     */
    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;

    /**
     * 실제 결제일 (CONFIRMED 시 설정)
     */
    @Column(name = "actual_date")
    private LocalDate actualDate;

    /**
     * 결제 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * 확정 시간 (CONFIRMED 시)
     */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 매칭 확정
     */
    public void confirm(Expense expense) {
        this.expense = expense;
        this.actualAmount = expense.getAmount();
        this.actualDate = expense.getDate();
        this.status = PaymentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    /**
     * 매칭 해제 (PENDING으로 복원)
     */
    public void unlink() {
        this.expense = null;
        this.actualAmount = null;
        this.actualDate = null;
        this.status = PaymentStatus.PENDING;
        this.confirmedAt = null;
    }

    /**
     * 건너뛰기
     */
    public void skip() {
        this.status = PaymentStatus.SKIPPED;
    }
}
