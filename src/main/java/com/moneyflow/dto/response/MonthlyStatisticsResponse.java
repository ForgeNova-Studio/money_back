package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 월간 통계 응답 DTO
 *
 * 기능:
 * - 총 지출 금액
 * - 카테고리별 지출 내역 (금액, 비율)
 * - 전월 대비 증감 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatisticsResponse {

    /**
     * 장부 ID
     */
    private UUID accountBookId;

    /**
     * 장부 이름
     */
    private String accountBookName;

    /**
     * 총 지출 금액
     */
    private BigDecimal totalAmount;

    /**
     * 카테고리별 지출 내역 리스트
     * 금액 기준 내림차순 정렬
     */
    private List<CategoryBreakdown> categoryBreakdown;

    /**
     * 전월 대비 증감 정보
     */
    private ComparisonData comparisonWithLastMonth;

    /**
     * 카테고리별 지출 상세 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        /**
         * 카테고리 코드 (FOOD, TRANSPORT 등)
         */
        private String category;

        /**
         * 해당 카테고리의 지출 금액
         */
        private BigDecimal amount;

        /**
         * 전체 지출 대비 비율 (0-100)
         */
        private double percentage;
    }

    /**
     * 전월 대비 증감 데이터
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonData {
        /**
         * 전월 대비 차이 금액 (양수: 증가, 음수: 감소)
         */
        private BigDecimal diff;

        /**
         * 전월 대비 증감률 (%) (양수: 증가, 음수: 감소)
         */
        private double diffPercentage;
    }
}
