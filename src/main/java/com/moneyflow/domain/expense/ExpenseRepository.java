package com.moneyflow.domain.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

        // 사용자별 기간별 지출 조회
        @Query("SELECT e FROM Expense e WHERE e.user.userId = :userId " +
                        "AND e.date BETWEEN :startDate AND :endDate " +
                        "AND (:category IS NULL OR e.category = :category) " +
                        "ORDER BY e.date DESC, e.createdAt DESC")
        List<Expense> findExpensesByUserAndDateRange(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("category") String category);

        // 커플별 기간별 지출 조회
        /**
         * @deprecated accountBookId 기반 쿼리를 사용하세요: findByAccountBookAndDateRange
         */
        @Deprecated
        @Query("SELECT e FROM Expense e WHERE e.coupleId = :coupleId " +
                        "AND e.date BETWEEN :startDate AND :endDate " +
                        "AND (:category IS NULL OR e.category = :category) " +
                        "ORDER BY e.date DESC, e.createdAt DESC")
        List<Expense> findExpensesByCoupleAndDateRange(
                        @Param("coupleId") UUID coupleId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("category") String category);

        // 장부별 기간별 지출 조회
        @Query("SELECT e FROM Expense e WHERE e.accountBook.accountBookId = :accountBookId " +
                        "AND e.date BETWEEN :startDate AND :endDate " +
                        "AND (:category IS NULL OR e.category = :category) " +
                        "ORDER BY e.date DESC, e.createdAt DESC")
        List<Expense> findByAccountBookAndDateRange(
                        @Param("accountBookId") UUID accountBookId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("category") String category);

        // 장부별 모든 지출 조회
        List<Expense> findByAccountBook_AccountBookIdOrderByDateDescCreatedAtDesc(UUID accountBookId);

        // 사용자별 최근 N개 조회
        List<Expense> findTop5ByUserUserIdOrderByDateDescCreatedAtDesc(UUID userId);

        // 특정 지출이 사용자의 것인지 확인
        boolean existsByExpenseIdAndUserUserId(UUID expenseId, UUID userId);

        // 특정 날짜 이후의 지출 조회 (구독료 탐지용)
        @Query("SELECT e FROM Expense e WHERE e.user.userId = :userId " +
                        "AND e.date >= :startDate " +
                        "ORDER BY e.date ASC")
        List<Expense> findByUser_UserIdAndDateAfter(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate);

        // 기간별 모든 지출 조회 (홈 화면 월간 데이터용)
        List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);

        // 사용자별 기간 지출 조회 (홈 화면 월간 데이터용)
        List<Expense> findByUser_UserIdAndDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);
}
