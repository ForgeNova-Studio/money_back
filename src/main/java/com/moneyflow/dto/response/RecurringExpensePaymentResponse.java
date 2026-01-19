package com.moneyflow.dto.response;

import com.moneyflow.domain.recurringexpense.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 월별 고정비 결제 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "월별 고정비 결제 정보")
public class RecurringExpensePaymentResponse {

    @Schema(description = "결제 ID")
    private UUID paymentId;

    @Schema(description = "고정비 ID")
    private UUID recurringExpenseId;

    @Schema(description = "고정비 이름")
    private String recurringExpenseName;

    @Schema(description = "카테고리")
    private String category;

    @Schema(description = "연결된 지출 ID")
    private UUID expenseId;

    @Schema(description = "기간 (년)")
    private Integer periodYear;

    @Schema(description = "기간 (월)")
    private Integer periodMonth;

    @Schema(description = "예상 금액")
    private BigDecimal expectedAmount;

    @Schema(description = "실제 금액 (확정 시)")
    private BigDecimal actualAmount;

    @Schema(description = "예상 결제일")
    private LocalDate expectedDate;

    @Schema(description = "실제 결제일 (확정 시)")
    private LocalDate actualDate;

    @Schema(description = "결제 상태", example = "PENDING")
    private PaymentStatus status;

    @Schema(description = "확정 시간")
    private LocalDateTime confirmedAt;
}
