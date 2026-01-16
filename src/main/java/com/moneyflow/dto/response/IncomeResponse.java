package com.moneyflow.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 수입 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeResponse {

    /**
     * 수입 ID
     */
    private UUID incomeId;

    /**
     * 사용자 ID
     */
    private UUID userId;

    /**
     * 소속 장부 ID
     */
    private UUID accountBookId;

    /**
     * 수입 출처 유형
     */
    private String fundingSource;

    /**
     * 수입 금액
     */
    private BigDecimal amount;

    /**
     * 수입 날짜
     */
    private LocalDate date;

    /**
     * 수입 출처
     */
    private String source;

    /**
     * 수입 설명
     */
    private String description;

    /**
     * 생성 일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    private LocalDateTime updatedAt;
}
