package com.moneyflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * 일괄 지출 생성 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "일괄 지출 생성 응답")
public class BulkExpenseResponse {

    @Schema(description = "전체 요청 개수")
    private int totalRequested;

    @Schema(description = "성공적으로 저장된 개수")
    private int successCount;

    @Schema(description = "실패한 개수")
    private int failedCount;

    @Schema(description = "저장된 지출 목록")
    private List<ExpenseResponse> savedExpenses;

    @Schema(description = "실패 상세 (있는 경우)")
    private List<FailedItem> failures;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FailedItem {
        @Schema(description = "인덱스 (0부터 시작)")
        private int index;

        @Schema(description = "실패 사유")
        private String reason;
    }
}
