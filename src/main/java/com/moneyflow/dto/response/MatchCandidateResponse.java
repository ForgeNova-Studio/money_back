package com.moneyflow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 매칭 후보 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "지출과 매칭 가능한 고정비 후보")
public class MatchCandidateResponse {

    @Schema(description = "결제 ID")
    private UUID paymentId;

    @Schema(description = "고정비 ID")
    private UUID recurringExpenseId;

    @Schema(description = "고정비 이름")
    private String name;

    @Schema(description = "카테고리")
    private String category;

    @Schema(description = "예상 금액")
    private BigDecimal expectedAmount;

    @Schema(description = "예상 결제일")
    private LocalDate expectedDate;

    @Schema(description = "매칭 점수 (0.0~1.0)")
    private Double matchScore;

    @Schema(description = "날짜 차이 (일)")
    private Integer daysDifference;

    @Schema(description = "금액 차이 (%)")
    private Double amountDifferencePercent;
}
