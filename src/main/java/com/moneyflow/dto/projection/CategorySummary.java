package com.moneyflow.dto.projection;

import java.math.BigDecimal;

/**
 * 카테고리별 집계 결과 DTO (Projection)
 *
 * DB GROUP BY 쿼리 결과를 받기 위한 인터페이스
 */
public interface CategorySummary {

    /**
     * 카테고리명 또는 출처명
     */
    String getName();

    /**
     * 합계 금액
     */
    BigDecimal getAmount();
}
