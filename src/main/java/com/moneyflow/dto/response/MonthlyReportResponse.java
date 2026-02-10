package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 월간 리포트 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportResponse {

    private Integer year;
    private Integer month;

    // 총 지출/수입
    private BigDecimal totalExpense;
    private BigDecimal totalIncome;
    private BigDecimal netIncome;

    // 전월 대비
    private BigDecimal previousMonthExpense;
    private Double changePercent; // 음수면 절약, 양수면 증가

    // 카테고리별 지출
    private List<CategoryBreakdown> categoryBreakdown;

    // TOP 3 지출
    private List<TopExpense> topExpenses;

    // 가장 많이 방문한 가맹점
    private TopMerchant topMerchant;

    // 예산 정보
    private BudgetSummary budget;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private String category;
        private BigDecimal amount;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopExpense {
        private String merchant;
        private BigDecimal amount;
        private LocalDate date;
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopMerchant {
        private String name;
        private Integer visitCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetSummary {
        private BigDecimal targetAmount;
        private BigDecimal currentSpending;
        private Integer usagePercentage;
    }
}
