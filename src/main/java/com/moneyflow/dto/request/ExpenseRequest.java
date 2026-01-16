package com.moneyflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {

    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "금액은 0보다 커야 합니다")
    private BigDecimal amount;

    @NotNull(message = "날짜는 필수입니다")
    private LocalDate date;

    /**
     * 카테고리 (선택) - 미입력 시 자동 분류
     */
    @Size(max = 50)
    private String category;

    @Size(max = 255)
    private String merchant;

    private String memo;

    @Size(max = 20)
    private String paymentMethod;

    private String imageUrl;

    /**
     * 소속 장부 ID
     */
    private UUID accountBookId;

    /**
     * 지출 출처 (PERSONAL: 개인, SHARED_POOL: 공금)
     */
    private com.moneyflow.domain.accountbook.FundingSource fundingSource;

    private Boolean isAutoCategorized;
}
