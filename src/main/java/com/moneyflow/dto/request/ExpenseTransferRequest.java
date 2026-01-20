package com.moneyflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * 지출 이동 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseTransferRequest {

    /**
     * 이동할 대상 장부 ID
     */
    @NotNull(message = "대상 장부 ID는 필수입니다")
    private UUID targetAccountBookId;
}
