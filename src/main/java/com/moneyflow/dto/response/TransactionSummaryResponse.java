package com.moneyflow.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 트랜잭션 요약 응답 DTO
 *
 * 홈 화면 월간 데이터의 각 거래 항목
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "트랜잭션 요약 정보")
public class TransactionSummaryResponse {

    @Schema(description = "트랜잭션 ID", example = "uuid-123")
    private String id;

    @Schema(description = "트랜잭션 유형", example = "EXPENSE", allowableValues = {"EXPENSE", "INCOME"})
    private String type;

    @Schema(description = "금액", example = "15000")
    private Integer amount;

    @Schema(description = "제목/내용", example = "스타벅스")
    private String title;

    @Schema(description = "카테고리 코드", example = "FOOD")
    private String category;

    @Schema(description = "카테고리 한글명", example = "식비")
    @JsonProperty("categoryName")
    private String categoryDisplayName;

    @Schema(description = "메모", example = "점심 커피", nullable = true)
    private String memo;
}
