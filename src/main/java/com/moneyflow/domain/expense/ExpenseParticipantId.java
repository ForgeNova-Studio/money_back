package com.moneyflow.domain.expense;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * 지출 참여자 복합 키
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExpenseParticipantId implements Serializable {

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "user_id")
    private UUID userId;
}
