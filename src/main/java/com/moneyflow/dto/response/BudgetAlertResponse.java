package com.moneyflow.dto.response;

import com.moneyflow.domain.budget.ThresholdType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 예산 알림 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAlertResponse {

    private UUID alertId;
    private UUID accountBookId;
    private Integer year;
    private Integer month;
    private ThresholdType thresholdType;
    private LocalDateTime triggeredAt;
}
