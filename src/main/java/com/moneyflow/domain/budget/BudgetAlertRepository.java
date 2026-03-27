package com.moneyflow.domain.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, UUID> {

    boolean existsByAccountBookIdAndYearAndMonthAndThresholdType(
            UUID accountBookId, Integer year, Integer month, ThresholdType thresholdType);

    java.util.List<BudgetAlert> findByAccountBookIdAndYearAndMonth(
            UUID accountBookId, Integer year, Integer month);
}
