package com.moneyflow.domain.recurringexpense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecurringExpensePaymentRepository extends JpaRepository<RecurringExpensePayment, UUID> {

    /**
     * 특정 고정비의 특정 월 결제 조회
     */
    Optional<RecurringExpensePayment> findByRecurringExpense_RecurringExpenseIdAndPeriodYearAndPeriodMonth(
            UUID recurringExpenseId, Integer periodYear, Integer periodMonth);

    /**
     * 특정 고정비의 특정 월 결제 존재 여부
     */
    boolean existsByRecurringExpense_RecurringExpenseIdAndPeriodYearAndPeriodMonth(
            UUID recurringExpenseId, Integer periodYear, Integer periodMonth);

    /**
     * 사용자의 특정 월 모든 결제 조회
     */
    @Query("SELECT p FROM RecurringExpensePayment p " +
            "JOIN FETCH p.recurringExpense re " +
            "WHERE re.user.userId = :userId " +
            "AND p.periodYear = :year AND p.periodMonth = :month " +
            "ORDER BY p.expectedDate")
    List<RecurringExpensePayment> findByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 사용자의 특정 월 PENDING 결제만 조회
     */
    @Query("SELECT p FROM RecurringExpensePayment p " +
            "JOIN FETCH p.recurringExpense re " +
            "WHERE re.user.userId = :userId " +
            "AND p.periodYear = :year AND p.periodMonth = :month " +
            "AND p.status = 'PENDING' " +
            "ORDER BY p.expectedDate")
    List<RecurringExpensePayment> findPendingByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 특정 장부의 PENDING 결제 조회 (매칭용)
     */
    @Query("SELECT p FROM RecurringExpensePayment p " +
            "JOIN FETCH p.recurringExpense re " +
            "WHERE re.accountBook.accountBookId = :accountBookId " +
            "AND p.periodYear = :year AND p.periodMonth = :month " +
            "AND p.status = 'PENDING' " +
            "ORDER BY p.expectedDate")
    List<RecurringExpensePayment> findPendingByAccountBookAndPeriod(
            @Param("accountBookId") UUID accountBookId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    /**
     * 연결된 지출 ID로 결제 조회
     */
    Optional<RecurringExpensePayment> findByExpense_ExpenseId(UUID expenseId);

    /**
     * 특정 고정비의 모든 결제 이력 (최근순)
     */
    @Query("SELECT p FROM RecurringExpensePayment p " +
            "WHERE p.recurringExpense.recurringExpenseId = :recurringExpenseId " +
            "ORDER BY p.periodYear DESC, p.periodMonth DESC")
    List<RecurringExpensePayment> findByRecurringExpenseOrderByPeriodDesc(
            @Param("recurringExpenseId") UUID recurringExpenseId);
}
