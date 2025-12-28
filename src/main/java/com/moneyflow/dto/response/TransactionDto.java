package com.moneyflow.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionDto {
    private Long id;
    private String type;   // "INCOME" or "EXPENSE"
    private long amount;
    private String title;
    private String category;
    private String time;   // "14:30"
}
