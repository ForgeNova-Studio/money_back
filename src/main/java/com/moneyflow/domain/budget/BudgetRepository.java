package com.moneyflow.domain.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
