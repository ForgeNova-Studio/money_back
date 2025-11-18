package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 예산 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetResponse {

    private UUID budgetId;
    private UUID userId;
    private Integer year;
    private Integer month;
    private BigDecimal targetAmount;
    private BigDecimal currentSpending;  // 현재 소비 금액
    private BigDecimal remainingAmount;  // 남은 금액
    private Double usagePercentage;      // 사용률 (%)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
