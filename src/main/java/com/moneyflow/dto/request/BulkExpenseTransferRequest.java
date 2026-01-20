package com.moneyflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * 일괄 지출 이동 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkExpenseTransferRequest {

    /**
     * 원본 장부 ID (여행 장부 등)
     */
    @NotNull(message = "원본 장부 ID는 필수입니다")
    private UUID sourceAccountBookId;

    /**
     * 이동할 대상 장부 ID (개인 장부)
     */
    @NotNull(message = "대상 장부 ID는 필수입니다")
    private UUID targetAccountBookId;
}
