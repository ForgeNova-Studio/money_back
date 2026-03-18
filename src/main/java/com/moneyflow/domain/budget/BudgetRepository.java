package com.moneyflow.domain.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 예산 저장소
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    /**
     * 가계부의 특정 년월 예산 조회
     */
    Optional<Budget> findByAccountBookAccountBookIdAndYearAndMonth(UUID accountBookId, Integer year, Integer month);

    /**
     * 특정 예산이 사용자의 것인지 확인
     */
    boolean existsByBudgetIdAndUserUserId(UUID budgetId, UUID userId);

    /**
     * 사용자의 모든 예산 삭제 (회원 탈퇴용)
     */
    @Modifying
    @Query("DELETE FROM Budget b WHERE b.user.userId = :userId")
    void deleteByUserUserId(@Param("userId") UUID userId);

    /**
     * 특정 장부들의 모든 예산 삭제 (회원 탈퇴용)
     */
    @Modifying
    @Query(value = "DELETE FROM budgets WHERE account_book_id IN (:accountBookIds)", nativeQuery = true)
    void deleteByAccountBookIdIn(@Param("accountBookIds") List<UUID> accountBookIds);
}
