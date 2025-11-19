package com.moneyflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 월간 고정비 총액 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "월간 고정비 총액 응답")
public class MonthlyRecurringTotalResponse {

    @Schema(description = "월간 고정비 총액", example = "125000")
    private BigDecimal monthlyTotal;

    @Schema(description = "고정비 항목 개수", example = "5")
    private Integer count;

    @Schema(description = "커플 모드 여부", example = "true")
    private Boolean isCoupleMode;
}
