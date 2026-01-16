package com.moneyflow.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private UUID expenseId;
    private UUID userId;
    private UUID accountBookId;
    private String fundingSource;
    private BigDecimal amount;
    private LocalDate date;
    private String category;
    private String merchant;
    private String memo;
    private String paymentMethod;
    private String imageUrl;
    private Boolean isAutoCategorized;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
