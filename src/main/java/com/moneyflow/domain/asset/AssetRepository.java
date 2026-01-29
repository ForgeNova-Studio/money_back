package com.moneyflow.domain.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    /**
     * 사용자의 모든 자산 조회 (생성일 내림차순)
     */
    List<Asset> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * 사용자의 특정 카테고리 자산 조회
     */
    List<Asset> findByUserUserIdAndCategoryOrderByCreatedAtDesc(UUID userId, AssetCategory category);

    /**
     * 사용자의 특정 자산 조회
     */
    Optional<Asset> findByAssetIdAndUserUserId(UUID assetId, UUID userId);

    /**
     * 사용자의 총 자산 합계
     */
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM Asset a WHERE a.user.userId = :userId")
    BigDecimal sumTotalAmount(@Param("userId") UUID userId);

    /**
     * 사용자의 카테고리별 자산 합계
     */
    @Query("SELECT a.category, COALESCE(SUM(a.amount), 0) " +
           "FROM Asset a WHERE a.user.userId = :userId " +
           "GROUP BY a.category")
    List<Object[]> sumAmountByCategory(@Param("userId") UUID userId);

    /**
     * 가계부별 자산 조회
     */
    List<Asset> findByAccountBookAccountBookIdOrderByCreatedAtDesc(UUID accountBookId);

    /**
     * 가계부별 총 자산 합계
     */
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM Asset a WHERE a.accountBook.accountBookId = :accountBookId")
    BigDecimal sumTotalAmountByAccountBook(@Param("accountBookId") UUID accountBookId);
}
