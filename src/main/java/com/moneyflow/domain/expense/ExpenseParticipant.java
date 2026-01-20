package com.moneyflow.domain.expense;

import com.moneyflow.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 지출 참여자 엔티티
 *
 * 공용 지출(SHARED_POOL)에서 N빵 대상자와 분담 비율을 관리합니다.
 * - 참여자가 지정되지 않으면 장부의 모든 멤버가 균등 분담
 * - 특정 인원만 참여한 경우 해당 인원만 정산 대상
 */
@Entity
@Table(name = "expense_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseParticipant {

    @EmbeddedId
    private ExpenseParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("expenseId")
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 분담 비율 (기본값 1.0 = 균등 분배)
     * 예: 3명 균등 분배 시 각각 1.0, 비율 분배 시 0.5, 0.3, 0.2 등
     */
    @Builder.Default
    @Column(name = "share_ratio", precision = 5, scale = 4)
    private BigDecimal shareRatio = BigDecimal.ONE;

    /**
     * 계산된 분담 금액
     * 정산 시 계산: expense.amount * (shareRatio / 총 참여자 비율 합)
     */
    @Column(name = "share_amount", precision = 12, scale = 2)
    private BigDecimal shareAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ===== 비즈니스 메서드 =====

    /**
     * 분담 금액 계산
     * 
     * @param totalExpenseAmount 지출 총액
     * @param totalRatioSum      모든 참여자의 비율 합계
     */
    public void calculateShareAmount(BigDecimal totalExpenseAmount, BigDecimal totalRatioSum) {
        if (totalRatioSum.compareTo(BigDecimal.ZERO) > 0) {
            this.shareAmount = totalExpenseAmount
                    .multiply(this.shareRatio)
                    .divide(totalRatioSum, 2, java.math.RoundingMode.HALF_UP);
        }
    }
}
