package com.moneyflow.dto.response;

import com.moneyflow.domain.asset.Asset;
import com.moneyflow.domain.asset.AssetCategory;
import com.moneyflow.domain.asset.AssetCategory.AssetGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 개별 자산 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetResponse {

    private UUID id;
    private String name;
    private AssetCategory category;
    private String categoryLabel;
    private AssetGroup group;
    private String groupLabel;
    private BigDecimal amount;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → Response DTO 변환
     */
    public static AssetResponse from(Asset asset) {
        return AssetResponse.builder()
                .id(asset.getAssetId())
                .name(asset.getName())
                .category(asset.getCategory())
                .categoryLabel(asset.getCategory().getLabel())
                .group(asset.getCategory().getGroup())
                .groupLabel(asset.getCategory().getGroup().getLabel())
                .amount(asset.getAmount())
                .memo(asset.getMemo())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .build();
    }
}
