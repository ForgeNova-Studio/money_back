package com.moneyflow.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 수입 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeListResponse {

    /**
     * 수입 목록
     */
    private List<IncomeResponse> incomes;

    /**
     * 총 수입 금액
     */
    private BigDecimal totalAmount;

    /**
     * 수입 개수
     */
    private int count;
}
