package com.moneyflow.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseListResponse {

    private List<ExpenseResponse> expenses;
    private BigDecimal totalAmount;
    private int count;
}
