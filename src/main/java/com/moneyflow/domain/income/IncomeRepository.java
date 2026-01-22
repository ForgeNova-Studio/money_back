package com.moneyflow.domain.income;

import com.moneyflow.dto.projection.CategorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 수입 레포지토리
 *
 * 수입 데이터에 대한 데이터베이스 액세스를 제공합니다.
 */
@Repository
public interface IncomeRepository extends JpaRepository<Income, UUID> {

        /**
         * 수입 ID로 조회 (User, AccountBook JOIN FETCH)
         * N+1 방지: toResponse()에서 user, accountBook 접근 시 추가 쿼리 방지
         */
        @Query("SELECT i FROM Income i " +
                        "LEFT JOIN FETCH i.user " +
                        "LEFT JOIN FETCH i.accountBook " +
                        "WHERE i.incomeId = :incomeId")
        Optional<Income> findByIdWithUserAndAccountBook(@Param("incomeId") UUID incomeId);

        /**
         * 사용자별 기간별 수입 조회 (User, AccountBook JOIN FETCH)
         * N+1 방지: 리스트 순회 시 user, accountBook 접근
         */
        @Query("SELECT i FROM Income i " +
                        "LEFT JOIN FETCH i.user " +
                        "LEFT JOIN FETCH i.accountBook " +
                        "WHERE i.user.userId = :userId " +
                        "AND i.date BETWEEN :startDate AND :endDate " +
                        "AND (:source IS NULL OR i.source = :source) " +
                        "ORDER BY i.date DESC, i.createdAt DESC")
        List<Income> findIncomesByUserAndDateRange(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("source") String source);

        /**
         * 장부별 기간별 수입 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT i FROM Income i " +
                        "LEFT JOIN FETCH i.user " +
                        "LEFT JOIN FETCH i.accountBook " +
                        "WHERE i.accountBook.accountBookId = :accountBookId " +
                        "AND i.date BETWEEN :startDate AND :endDate " +
                        "AND (:source IS NULL OR i.source = :source) " +
                        "ORDER BY i.date DESC, i.createdAt DESC")
        List<Income> findByAccountBookAndDateRange(
                        @Param("accountBookId") UUID accountBookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("source") String source);

        /**
         * 장부별 모든 수입 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT i FROM Income i " +
                        "LEFT JOIN FETCH i.user " +
                        "LEFT JOIN FETCH i.accountBook " +
                        "WHERE i.accountBook.accountBookId = :accountBookId " +
                        "ORDER BY i.date DESC, i.createdAt DESC")
        List<Income> findByAccountBookIdWithFetch(@Param("accountBookId") UUID accountBookId);

        /**
         * 사용자별 최근 5개 수입 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT i FROM Income i " +
                        "LEFT JOIN FETCH i.user " +
                        "LEFT JOIN FETCH i.accountBook " +
                        "WHERE i.user.userId = :userId " +
                        "ORDER BY i.date DESC, i.createdAt DESC " +
                        "LIMIT 5")
        List<Income> findTop5ByUserUserIdOrderByDateDescCreatedAtDesc(@Param("userId") UUID userId);

        /**
         * 특정 수입이 사용자의 것인지 확인
         */
        boolean existsByIncomeIdAndUserUserId(UUID incomeId, UUID userId);

        /**
         * 기간별 모든 수입 조회 (홈 화면 월간 데이터용)
         */
        @Query("SELECT i FROM Income i " +
                        "LEFT JOIN FETCH i.user " +
                        "LEFT JOIN FETCH i.accountBook " +
                        "WHERE i.date BETWEEN :startDate AND :endDate")
        List<Income> findByDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * 사용자별 기간 수입 조회 (홈 화면 월간 데이터용)
         */
        @Query("SELECT i FROM Income i " +
                        "LEFT JOIN FETCH i.user " +
                        "LEFT JOIN FETCH i.accountBook " +
                        "WHERE i.user.userId = :userId " +
                        "AND i.date BETWEEN :startDate AND :endDate")
        List<Income> findByUser_UserIdAndDateBetween(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // ==================== 자산 현황 계산용 ====================

        /**
         * [총자산용] 장부별 전체 수입 합계 (조건 없는 누적)
         */
        @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i WHERE i.accountBook.accountBookId = :bookId")
        java.math.BigDecimal sumTotalAmount(@Param("bookId") UUID bookId);

        /**
         * [기간 분석용] 장부별 특정 기간 내 수입 합계
         */
        @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Income i " +
                        "WHERE i.accountBook.accountBookId = :bookId " +
                        "AND i.date BETWEEN :startDate AND :endDate")
        java.math.BigDecimal sumAmountByPeriod(
                        @Param("bookId") UUID bookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * [출처별 집계] 장부별 기간 내 수입 출처별 합계 (DB GROUP BY)
         *
         * Java 메모리 연산 대신 DB에서 직접 집계하여 성능 최적화
         * 예: 1,000건 수입 → 5개 출처 결과만 반환
         */
        @Query("SELECT i.source AS name, SUM(i.amount) AS amount " +
                        "FROM Income i " +
                        "WHERE i.accountBook.accountBookId = :bookId " +
                        "AND i.date BETWEEN :startDate AND :endDate " +
                        "GROUP BY i.source " +
                        "ORDER BY SUM(i.amount) DESC")
        List<CategorySummary> sumBySource(
                        @Param("bookId") UUID bookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
