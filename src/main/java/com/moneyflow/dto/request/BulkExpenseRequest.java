package com.moneyflow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/**
 * 일괄 지출 생성 요청 DTO
 * OCR 결과를 한 번에 저장할 때 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "일괄 지출 생성 요청")
public class BulkExpenseRequest {

    @NotEmpty(message = "최소 1개 이상의 지출 항목이 필요합니다")
    @Valid
    @Schema(description = "저장할 지출 목록")
    private List<ExpenseRequest> expenses;
}
