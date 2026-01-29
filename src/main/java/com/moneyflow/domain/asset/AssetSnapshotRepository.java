package com.moneyflow.domain.asset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * 자산 스냅샷 리포지토리
 */
@Repository
public interface AssetSnapshotRepository extends JpaRepository<AssetSnapshot, UUID> {

    /**
     * 특정 가계부의 특정 날짜 스냅샷 조회
     */
    Optional<AssetSnapshot> findByAccountBook_AccountBookIdAndSnapshotDate(
            UUID accountBookId, LocalDate snapshotDate);

    /**
     * 특정 가계부의 특정 날짜 이전 가장 최근 스냅샷 조회
     * (지난달 말 스냅샷 찾을 때 사용)
     */
    @Query("""
            SELECT s FROM AssetSnapshot s
            WHERE s.accountBook.accountBookId = :accountBookId
              AND s.snapshotDate <= :beforeDate
            ORDER BY s.snapshotDate DESC
            LIMIT 1
            """)
    Optional<AssetSnapshot> findLatestBeforeDate(
            @Param("accountBookId") UUID accountBookId,
            @Param("beforeDate") LocalDate beforeDate);

    /**
     * 특정 가계부의 스냅샷 존재 여부 확인
     */
    boolean existsByAccountBook_AccountBookIdAndSnapshotDate(
            UUID accountBookId, LocalDate snapshotDate);

    /**
     * 오래된 스냅샷 삭제 (90일 이상 된 데이터)
     */
    @Query("""
            DELETE FROM AssetSnapshot s
            WHERE s.snapshotDate < :cutoffDate
            """)
    void deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}
