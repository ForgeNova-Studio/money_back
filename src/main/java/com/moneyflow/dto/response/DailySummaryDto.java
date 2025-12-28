package com.moneyflow.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class DailySummaryDto {
    private String date;          // "2025-12-24"
    private long totalIncome;     // 수입 합계
    private long totalExpense;    // 지출 합계
    private List<TransactionDto> transactions; // 그 날의 내역 리스트
}
