package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 카테고리별 전월 대비 변화 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryMonthlyComparisonResponse {

    private UUID accountBookId;

    private String accountBookName;

    private int year;

    private int month;

    /**
     * 이번 달 총 지출
     */
    private BigDecimal currentMonthTotal;

    /**
     * 전월 총 지출
     */
    private BigDecimal previousMonthTotal;

    /**
     * 카테고리별 전월 대비 변화 리스트 (이번 달 금액 기준 내림차순)
     */
    private List<CategoryComparison> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryComparison {

        /**
         * 카테고리 코드 (FOOD, TRANSPORT 등)
         */
        private String category;

        /**
         * 이번 달 지출 금액
         */
        private BigDecimal currentAmount;

        /**
         * 전월 지출 금액
         */
        private BigDecimal previousAmount;

        /**
         * 전월 대비 차이 금액 (양수: 증가, 음수: 감소)
         */
        private BigDecimal diff;

        /**
         * 전월 대비 증감률 (%) (양수: 증가, 음수: 감소, 전월 0이면 null)
         */
        private Double diffPercentage;
    }
}
