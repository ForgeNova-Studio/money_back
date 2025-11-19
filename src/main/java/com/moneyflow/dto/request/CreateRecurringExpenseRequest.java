package com.moneyflow.dto.request;

import com.moneyflow.domain.recurringexpense.RecurringType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 고정비/구독료 등록 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고정비/구독료 등록 요청")
public class CreateRecurringExpenseRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이하여야 합니다")
    @Schema(description = "고정비 이름", example = "넷플릭스", required = true)
    private String name;

    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다")
    @Schema(description = "금액", example = "13500", required = true)
    private BigDecimal amount;

    @NotBlank(message = "카테고리는 필수입니다")
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    @Schema(description = "카테고리", example = "문화/여가", required = true)
    private String category;

    @Schema(description = "설명", example = "스탠다드 플랜")
    private String description;

    @NotNull(message = "반복 유형은 필수입니다")
    @Schema(description = "반복 주기 (MONTHLY, YEARLY, WEEKLY)", example = "MONTHLY", required = true)
    private RecurringType recurringType;

    @NotNull(message = "시작일은 필수입니다")
    @Schema(description = "시작일", example = "2025-01-01", required = true)
    private LocalDate startDate;

    @Schema(description = "종료일 (null이면 무기한)", example = "2026-12-31")
    private LocalDate endDate;

    @Min(value = 1, message = "매달 날짜는 1~31 사이여야 합니다")
    @Max(value = 31, message = "매달 날짜는 1~31 사이여야 합니다")
    @Schema(description = "매달 몇일에 결제 (MONTHLY일 때만)", example = "15")
    private Integer dayOfMonth;

    @Min(value = 0, message = "요일은 0~6 사이여야 합니다")
    @Max(value = 6, message = "요일은 0~6 사이여야 합니다")
    @Schema(description = "요일 (0=일요일, 6=토요일, WEEKLY일 때만)", example = "1")
    private Integer dayOfWeek;

    @NotNull(message = "다음 결제일은 필수입니다")
    @Schema(description = "다음 결제 예정일", example = "2025-02-15", required = true)
    private LocalDate nextPaymentDate;

    @Schema(description = "구독료 여부", example = "true")
    private Boolean isSubscription = false;

    @Size(max = 100, message = "구독 제공자는 100자 이하여야 합니다")
    @Schema(description = "구독 제공자 (넷플릭스, 스포티파이 등)", example = "넷플릭스")
    private String subscriptionProvider;

    @Schema(description = "알림 활성화 여부", example = "true")
    private Boolean notificationEnabled = true;
}
