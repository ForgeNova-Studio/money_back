package com.moneyflow.dto.response;

import com.moneyflow.domain.asset.AssetCategory;
import com.moneyflow.domain.asset.AssetCategory.AssetGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 자산 요약 응답 DTO
 * 프론트엔드 AssetSummary와 매핑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetSummaryResponse {

    /**
     * 총 자산 금액
     */
    private BigDecimal totalAmount;

    /**
     * 전월 대비 변화 금액
     */
    private BigDecimal previousMonthDiff;

    /**
     * 그룹별 요약 (Level 1: 현금성/투자/실물/기타)
     */
    private List<GroupBreakdownDto> groupBreakdowns;

    /**
     * 카테고리별 요약 (Level 2: 개별 카테고리)
     */
    private List<CategoryBreakdownDto> categoryBreakdowns;

    /**
     * 개별 자산 목록
     */
    private List<AssetResponse> assets;

    /**
     * 그룹별 자산 요약 (Level 1)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupBreakdownDto {
        private AssetGroup group;
        private String groupLabel;
        private BigDecimal amount;
        private double percent;
        private List<CategoryBreakdownDto> categories;
    }

    /**
     * 카테고리별 자산 요약 (Level 2)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryBreakdownDto {
        private AssetCategory category;
        private String categoryLabel;
        private AssetGroup group;
        private String groupLabel;
        private BigDecimal amount;
        private double percent;
    }
}
