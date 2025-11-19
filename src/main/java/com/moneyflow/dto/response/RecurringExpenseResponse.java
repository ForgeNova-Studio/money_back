package com.moneyflow.dto.response;

import com.moneyflow.domain.recurringexpense.RecurringType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 고정비/구독료 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고정비/구독료 응답")
public class RecurringExpenseResponse {

    @Schema(description = "고정비 ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID recurringExpenseId;

    @Schema(description = "사용자 ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;

    @Schema(description = "커플 ID (null이면 개인 고정비)", example = "123e4567-e89b-12d3-a456-426614174002")
    private UUID coupleId;

    @Schema(description = "고정비 이름", example = "넷플릭스")
    private String name;

    @Schema(description = "금액", example = "13500")
    private BigDecimal amount;

    @Schema(description = "카테고리", example = "문화/여가")
    private String category;

    @Schema(description = "설명", example = "스탠다드 플랜")
    private String description;

    @Schema(description = "반복 주기", example = "MONTHLY")
    private RecurringType recurringType;

    @Schema(description = "시작일", example = "2025-01-01")
    private LocalDate startDate;

    @Schema(description = "종료일 (null이면 무기한)", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "매달 몇일에 결제 (MONTHLY일 때)", example = "15")
    private Integer dayOfMonth;

    @Schema(description = "요일 (0=일요일, 6=토요일, WEEKLY일 때)", example = "1")
    private Integer dayOfWeek;

    @Schema(description = "다음 결제 예정일", example = "2025-02-15")
    private LocalDate nextPaymentDate;

    @Schema(description = "구독료 여부", example = "true")
    private Boolean isSubscription;

    @Schema(description = "구독 제공자", example = "넷플릭스")
    private String subscriptionProvider;

    @Schema(description = "알림 활성화 여부", example = "true")
    private Boolean notificationEnabled;

    @Schema(description = "이전 금액 (변동 감지용)", example = "9500")
    private BigDecimal lastAmount;

    @Schema(description = "마지막 결제일", example = "2025-01-15")
    private LocalDate lastPaymentDate;

    @Schema(description = "자동 탐지 여부", example = "true")
    private Boolean autoDetected;

    @Schema(description = "탐지 신뢰도 (0.00 ~ 1.00)", example = "0.95")
    private BigDecimal detectionConfidence;

    @Schema(description = "생성일시", example = "2025-01-01T12:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-15T15:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "금액 변동 여부", example = "true")
    private Boolean hasAmountChanged;

    @Schema(description = "만료 여부", example = "false")
    private Boolean isExpired;
}
