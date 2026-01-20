package com.moneyflow.domain.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * 지출 참여자 리포지토리
 */
@Repository
public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, ExpenseParticipantId> {

    /**
     * 특정 지출의 모든 참여자 조회 (User JOIN FETCH)
     */
    @Query("SELECT ep FROM ExpenseParticipant ep " +
            "JOIN FETCH ep.user " +
            "WHERE ep.expense.expenseId = :expenseId")
    List<ExpenseParticipant> findByExpenseId(@Param("expenseId") UUID expenseId);

    /**
     * 특정 사용자가 참여한 모든 지출 참여 정보 조회
     */
    @Query("SELECT ep FROM ExpenseParticipant ep " +
            "JOIN FETCH ep.expense e " +
            "JOIN FETCH ep.user " +
            "WHERE ep.user.userId = :userId")
    List<ExpenseParticipant> findByUserId(@Param("userId") UUID userId);

    /**
     * 특정 장부의 모든 공용 지출 참여 정보 조회 (정산 계산용)
     */
    @Query("SELECT ep FROM ExpenseParticipant ep " +
            "JOIN FETCH ep.expense e " +
            "JOIN FETCH ep.user " +
            "LEFT JOIN FETCH e.paidBy " +
            "WHERE e.accountBook.accountBookId = :accountBookId " +
            "AND e.fundingSource = 'SHARED_POOL'")
    List<ExpenseParticipant> findByAccountBookIdForSettlement(@Param("accountBookId") UUID accountBookId);

    /**
     * 지출 ID로 참여자 삭제
     */
    void deleteByExpenseExpenseId(UUID expenseId);
}
