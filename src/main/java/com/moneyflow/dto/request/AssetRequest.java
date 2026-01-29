package com.moneyflow.dto.request;

import com.moneyflow.domain.asset.AssetCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 자산 생성/수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetRequest {

    /**
     * 가계부 ID (선택, 미지정 시 기본 가계부 사용)
     */
    private UUID accountBookId;

    /**
     * 자산명 (예: "비상금 통장", "삼성전자")
     */
    @NotBlank(message = "자산명은 필수입니다")
    @Size(max = 100, message = "자산명은 100자 이하여야 합니다")
    private String name;

    /**
     * 자산 카테고리
     */
    @NotNull(message = "카테고리는 필수입니다")
    private AssetCategory category;

    /**
     * 현재 금액
     */
    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0.0", message = "금액은 0 이상이어야 합니다")
    private BigDecimal amount;

    /**
     * 메모 (선택)
     */
    private String memo;
}
