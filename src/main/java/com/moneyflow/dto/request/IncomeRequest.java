package com.moneyflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 수입 생성/수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeRequest {

    /**
     * 수입 금액
     */
    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "금액은 0보다 커야 합니다")
    private BigDecimal amount;

    /**
     * 수입 날짜
     */
    @NotNull(message = "날짜는 필수입니다")
    private LocalDate date;

    /**
     * 수입 출처
     * 예: 급여, 부수입, 상여금, 투자수익, 기타
     */
    @NotBlank(message = "수입 출처는 필수입니다")
    @Size(max = 50, message = "수입 출처는 최대 50자까지 입력 가능합니다")
    private String source;

    /**
     * 수입 설명 (선택)
     */
    @Size(max = 1000, message = "설명은 최대 1000자까지 입력 가능합니다")
    private String description;

    /**
     * 커플 ID (커플 모드인 경우)
     */
    private UUID coupleId;
}
