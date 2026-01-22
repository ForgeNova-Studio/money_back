package com.moneyflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 자산 현황 응답 DTO
 * 
 * 총자산, 기간별 손익, 카테고리별 통계를 포함합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "자산 현황 응답")
public class TotalAssetResponse {

    @Schema(description = "조회한 장부 ID")
    private UUID accountBookId;

    @Schema(description = "조회한 장부 이름")
    private String accountBookName;

    // ==================== 현재 자산 상태 (날짜 필터와 무관) ====================

    @Schema(description = "현재 총자산 (초기잔액 + 총수입 - 총지출)", example = "5000000")
    private BigDecimal currentTotalAssets;

    @Schema(description = "장부 시작 시 초기 잔액", example = "1000000")
    private BigDecimal initialBalance;

    @Schema(description = "누적 총수입", example = "6000000")
    private BigDecimal totalIncome;

    @Schema(description = "누적 총지출", example = "2000000")
    private BigDecimal totalExpense;

    // ==================== 선택한 기간 동안의 흐름 ====================

    @Schema(description = "조회 시작일")
    private LocalDate filterStartDate;

    @Schema(description = "조회 종료일")
    private LocalDate filterEndDate;

    @Schema(description = "기간 내 수입 합계", example = "3000000")
    private BigDecimal periodIncome;

    @Schema(description = "기간 내 지출 합계", example = "1500000")
    private BigDecimal periodExpense;

    @Schema(description = "기간 내 순수익 (수입 - 지출)", example = "1500000")
    private BigDecimal periodNetIncome;

    // ==================== 그래프용 데이터 (선택적) ====================

    @Schema(description = "기간 내 수입 출처별 통계")
    private List<CategoryStat> incomeStats;

    @Schema(description = "기간 내 지출 카테고리별 통계")
    private List<CategoryStat> expenseStats;

    /**
     * 카테고리/출처별 통계 데이터
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "카테고리별 통계")
    public static class CategoryStat {

        @Schema(description = "카테고리명 또는 출처", example = "급여")
        private String name;

        @Schema(description = "금액", example = "2500000")
        private BigDecimal amount;

        @Schema(description = "전체 대비 비중 (%)", example = "45.5")
        private double percent;
    }
}
