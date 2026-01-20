package com.moneyflow.domain.recurringexpense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 고정비 및 구독료 Repository
 */
@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, UUID> {

        /**
         * 고정비 ID로 조회 (User, AccountBook JOIN FETCH)
         * N+1 방지: toResponse()에서 user, accountBook 접근 시 추가 쿼리 방지
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.recurringExpenseId = :id")
        Optional<RecurringExpense> findByIdWithUserAndAccountBook(@Param("id") UUID id);

        /**
         * 사용자의 모든 고정비 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId")
        List<RecurringExpense> findByUser_UserId(@Param("userId") UUID userId);

        /**
         * 장부별 고정비 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.accountBook.accountBookId = :accountBookId")
        List<RecurringExpense> findByAccountBook_AccountBookId(@Param("accountBookId") UUID accountBookId);

        /**
         * 사용자가 멤버인 모든 장부의 고정비 + 개인 고정비(장부 없음) 조회
         * (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId " +
                        "OR r.accountBook.accountBookId IN (" +
                        "  SELECT m.accountBook.accountBookId FROM AccountBookMember m WHERE m.user.userId = :userId)")
        List<RecurringExpense> findByUserOrSharedAccountBooks(@Param("userId") UUID userId);

        /**
         * 활성 고정비 조회 (종료되지 않은 것, 공유 장부 포함)
         * (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE (r.user.userId = :userId OR r.accountBook.accountBookId IN (" +
                        "  SELECT m.accountBook.accountBookId FROM AccountBookMember m WHERE m.user.userId = :userId)) "
                        +
                        "AND (r.endDate IS NULL OR r.endDate >= CURRENT_DATE)")
        List<RecurringExpense> findActiveRecurringExpensesWithSharedBooks(@Param("userId") UUID userId);

        /**
         * 다가오는 결제 조회 (공유 장부 포함)
         * (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE (r.user.userId = :userId OR r.accountBook.accountBookId IN (" +
                        "  SELECT m.accountBook.accountBookId FROM AccountBookMember m WHERE m.user.userId = :userId)) "
                        +
                        "AND r.nextPaymentDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY r.nextPaymentDate ASC")
        List<RecurringExpense> findUpcomingPaymentsWithSharedBooks(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * 월간 고정비 총액 (공유 장부 포함)
         */
        @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RecurringExpense r WHERE " +
                        "(r.user.userId = :userId OR r.accountBook.accountBookId IN (" +
                        "  SELECT m.accountBook.accountBookId FROM AccountBookMember m WHERE m.user.userId = :userId)) "
                        +
                        "AND r.recurringType = 'MONTHLY' " +
                        "AND (r.endDate IS NULL OR r.endDate >= CURRENT_DATE)")
        BigDecimal calculateMonthlyTotalWithSharedBooks(@Param("userId") UUID userId);

        // ========== 개인 전용 쿼리 ==========

        /**
         * 구독료만 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId AND r.isSubscription = :isSubscription")
        List<RecurringExpense> findByUser_UserIdAndIsSubscription(
                        @Param("userId") UUID userId,
                        @Param("isSubscription") Boolean isSubscription);

        /**
         * 특정 기간 내 결제 예정인 고정비 조회 (개인만, User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId " +
                        "AND r.nextPaymentDate BETWEEN :startDate AND :endDate " +
                        "ORDER BY r.nextPaymentDate ASC")
        List<RecurringExpense> findUpcomingPayments(
                        @Param("userId") UUID userId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /**
         * 특정 카테고리의 고정비 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId AND r.category = :category")
        List<RecurringExpense> findByUser_UserIdAndCategory(
                        @Param("userId") UUID userId,
                        @Param("category") String category);

        /**
         * 종료되지 않은 고정비만 조회 (개인만, User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId " +
                        "AND (r.endDate IS NULL OR r.endDate >= CURRENT_DATE)")
        List<RecurringExpense> findActiveRecurringExpenses(@Param("userId") UUID userId);

        /**
         * 월간 고정비 총액 계산 (개인만)
         */
        @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RecurringExpense r " +
                        "WHERE r.user.userId = :userId " +
                        "AND r.recurringType = 'MONTHLY' " +
                        "AND (r.endDate IS NULL OR r.endDate >= CURRENT_DATE)")
        BigDecimal calculateMonthlyTotal(@Param("userId") UUID userId);

        /**
         * 알림이 활성화된 고정비 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId " +
                        "AND r.notificationEnabled = true " +
                        "AND r.nextPaymentDate = :date")
        List<RecurringExpense> findNotificationEnabledByDate(
                        @Param("userId") UUID userId,
                        @Param("date") LocalDate date);

        /**
         * 자동 탐지된 고정비 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId AND r.autoDetected = :autoDetected")
        List<RecurringExpense> findByUser_UserIdAndAutoDetected(
                        @Param("userId") UUID userId,
                        @Param("autoDetected") Boolean autoDetected);

        /**
         * 특정 구독 제공자의 고정비 조회 (User, AccountBook JOIN FETCH)
         */
        @Query("SELECT r FROM RecurringExpense r " +
                        "LEFT JOIN FETCH r.user " +
                        "LEFT JOIN FETCH r.accountBook " +
                        "WHERE r.user.userId = :userId AND r.subscriptionProvider = :subscriptionProvider")
        Optional<RecurringExpense> findByUser_UserIdAndSubscriptionProvider(
                        @Param("userId") UUID userId,
                        @Param("subscriptionProvider") String subscriptionProvider);
}
