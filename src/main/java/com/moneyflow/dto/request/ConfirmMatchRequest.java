package com.moneyflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * 매칭 확정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고정비 결제와 지출 매칭 확정 요청")
public class ConfirmMatchRequest {

    @NotNull(message = "지출 ID는 필수입니다")
    @Schema(description = "연결할 지출 ID", required = true)
    private UUID expenseId;
}
