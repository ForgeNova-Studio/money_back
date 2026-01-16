package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 주간 통계 응답 DTO
 *
 * 기능:
 * - 일별 지출 금액 (최근 7일)
 * - 최다 지출 카테고리
 * - 일평균 지출 금액
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatisticsResponse {

    /**
     * 장부 ID
     */
    private UUID accountBookId;

    /**
     * 장부 이름
     */
    private String accountBookName;

    /**
     * 일별 지출 내역 리스트
     * 날짜 기준 오름차순 정렬
     */
    private List<DailyExpense> dailyExpenses;

    /**
     * 최다 지출 카테고리 (금액 기준)
     */
    private String topCategory;

    /**
     * 일평균 지출 금액
     */
    private BigDecimal averageDaily;

    /**
     * 일별 지출 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyExpense {
        /**
         * 날짜
         */
        private LocalDate date;

        /**
         * 해당 날짜의 총 지출 금액
         */
        private BigDecimal amount;
    }
}
