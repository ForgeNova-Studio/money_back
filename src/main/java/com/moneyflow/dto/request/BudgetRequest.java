package com.moneyflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 예산 생성/수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetRequest {

    @NotNull(message = "가계부 ID는 필수입니다")
    private UUID accountBookId;

    @NotNull(message = "년도는 필수입니다")
    @Min(value = 2020, message = "년도는 2020년 이상이어야 합니다")
    @Max(value = 2100, message = "년도는 2100년 이하여야 합니다")
    private Integer year;

    @NotNull(message = "월은 필수입니다")
    @Min(value = 1, message = "월은 1 이상이어야 합니다")
    @Max(value = 12, message = "월은 12 이하여야 합니다")
    private Integer month;

    @NotNull(message = "목표 금액은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = true, message = "목표 금액은 0 이상이어야 합니다")
    private BigDecimal targetAmount;
}
