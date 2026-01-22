package com.moneyflow.domain.expense;

import com.moneyflow.dto.projection.CategorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

        /**
         * 지출 ID로 조회 (User, AccountBook JOIN FETCH)
         * N+1 방지: toResponse()에서 user, accountBook 접근 시 추가 쿼리 방지
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH e.accountBook " +
                        "WHERE e.expenseId = :expenseId")
        Optional<Expense> findByIdWithUserAndAccountBook(@Param("expenseId") UUID expenseId);

        /**
         * 사용자별 기간별 지출 조회 (User, AccountBook JOIN FETCH)
         * N+1 방지: 리스트 순회 시 user, accountBook 접근
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH e.accountBook " +
                        "WHERE e.user.userId = :userId " +
                        "AND e.date BETWEEN :startDate AND :endDate " +
                        "AND (:category IS NULL OR e.category = :category) " +
                        "ORDER BY e.date DESC, e.createdAt DESC")
        List<Expense> findExpensesByUserAndDateRange(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("category") String category);

        /**
         * 장부별 기간별 지출 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH e.accountBook " +
                        "WHERE e.accountBook.accountBookId = :accountBookId " +
                        "AND e.date BETWEEN :startDate AND :endDate " +
                        "AND (:category IS NULL OR e.category = :category) " +
                        "ORDER BY e.date DESC, e.createdAt DESC")
        List<Expense> findByAccountBookAndDateRange(
                        @Param("accountBookId") UUID accountBookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("category") String category);

        /**
         * 장부별 모든 지출 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH e.accountBook " +
                        "WHERE e.accountBook.accountBookId = :accountBookId " +
                        "ORDER BY e.date DESC, e.createdAt DESC")
        List<Expense> findByAccountBookIdWithFetch(@Param("accountBookId") UUID accountBookId);

        /**
         * 사용자별 최근 N개 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH e.accountBook " +
                        "WHERE e.user.userId = :userId " +
                        "ORDER BY e.date DESC, e.createdAt DESC " +
                        "LIMIT 5")
        List<Expense> findTop5ByUserUserIdOrderByDateDescCreatedAtDesc(@Param("userId") UUID userId);

        /**
         * 특정 지출이 사용자의 것인지 확인
         */
        boolean existsByExpenseIdAndUserUserId(UUID expenseId, UUID userId);

        /**
         * 특정 날짜 이후의 지출 조회 (구독료 탐지용)
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "WHERE e.user.userId = :userId " +
                        "AND e.date >= :startDate " +
                        "ORDER BY e.date ASC")
        List<Expense> findByUser_UserIdAndDateAfter(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate);

        /**
         * 기간별 모든 지출 조회 (홈 화면 월간 데이터용)
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH e.accountBook " +
                        "WHERE e.date BETWEEN :startDate AND :endDate")
        List<Expense> findByDateBetween(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * 사용자별 기간 지출 조회 (홈 화면 월간 데이터용)
         */
        @Query("SELECT e FROM Expense e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH e.accountBook " +
                        "WHERE e.user.userId = :userId " +
                        "AND e.date BETWEEN :startDate AND :endDate")
        List<Expense> findByUser_UserIdAndDateBetween(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // ==================== 자산 현황 계산용 ====================

        /**
         * [총자산용] 장부별 전체 지출 합계 (조건 없는 누적)
         */
        @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.accountBook.accountBookId = :bookId")
        java.math.BigDecimal sumTotalAmount(@Param("bookId") UUID bookId);

        /**
         * [기간 분석용] 장부별 특정 기간 내 지출 합계
         */
        @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
                        "WHERE e.accountBook.accountBookId = :bookId " +
                        "AND e.date BETWEEN :startDate AND :endDate")
        java.math.BigDecimal sumAmountByPeriod(
                        @Param("bookId") UUID bookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * [카테고리별 집계] 장부별 기간 내 카테고리별 지출 합계 (DB GROUP BY)
         *
         * Java 메모리 연산 대신 DB에서 직접 집계하여 성능 최적화
         * 예: 3,000건 지출 → 9개 카테고리 결과만 반환
         */
        @Query("SELECT e.category AS name, SUM(e.amount) AS amount " +
                        "FROM Expense e " +
                        "WHERE e.accountBook.accountBookId = :bookId " +
                        "AND e.date BETWEEN :startDate AND :endDate " +
                        "GROUP BY e.category " +
                        "ORDER BY SUM(e.amount) DESC")
        List<CategorySummary> sumByCategory(
                        @Param("bookId") UUID bookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
