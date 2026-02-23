package com.moneyflow.service;

import com.moneyflow.domain.budget.Budget;
import com.moneyflow.domain.budget.BudgetRepository;
import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.income.IncomeRepository;
import com.moneyflow.dto.projection.CategorySummary;
import com.moneyflow.dto.response.MonthlyReportResponse;
import com.moneyflow.dto.response.MonthlyReportResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 월간 리포트 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

        private final ExpenseRepository expenseRepository;
        private final IncomeRepository incomeRepository;
        private final BudgetRepository budgetRepository;

        /**
         * 월간 리포트 생성
         */
        public MonthlyReportResponse getMonthlyReport(UUID accountBookId, int year, int month) {
                YearMonth yearMonth = YearMonth.of(year, month);
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth();

                // 이번 달 지출/수입 합계
                BigDecimal totalExpense = expenseRepository.sumAmountByPeriod(accountBookId, startDate, endDate);
                BigDecimal totalIncome = incomeRepository.sumAmountByPeriod(accountBookId, startDate, endDate);
                BigDecimal netIncome = totalIncome.subtract(totalExpense);

                // 전월 지출 (비교용)
                YearMonth prevMonth = yearMonth.minusMonths(1);
                LocalDate prevStart = prevMonth.atDay(1);
                LocalDate prevEnd = prevMonth.atEndOfMonth();
                BigDecimal previousMonthExpense = expenseRepository.sumAmountByPeriod(accountBookId, prevStart,
                                prevEnd);

                // 변화율 계산
                Double changePercent = calculateChangePercent(totalExpense, previousMonthExpense);

                // 카테고리별 지출
                List<CategoryBreakdown> categoryBreakdown = getCategoryBreakdown(accountBookId, startDate, endDate,
                                totalExpense);

                // TOP 3 지출
                List<TopExpense> topExpenses = getTopExpenses(accountBookId, startDate, endDate);

                // 가장 많이 방문한 가맹점
                TopMerchant topMerchant = getTopMerchant(accountBookId, startDate, endDate);

                // 예산 정보
                BudgetSummary budget = getBudgetSummary(accountBookId, year, month, totalExpense);

                return MonthlyReportResponse.builder()
                                .year(year)
                                .month(month)
                                .totalExpense(totalExpense)
                                .totalIncome(totalIncome)
                                .netIncome(netIncome)
                                .previousMonthExpense(previousMonthExpense)
                                .changePercent(changePercent)
                                .categoryBreakdown(categoryBreakdown)
                                .topExpenses(topExpenses)
                                .topMerchant(topMerchant)
                                .budget(budget)
                                .build();
        }

        private Double calculateChangePercent(BigDecimal current, BigDecimal previous) {
                if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                }
                BigDecimal diff = current.subtract(previous);
                return diff.multiply(BigDecimal.valueOf(100))
                                .divide(previous, 1, RoundingMode.HALF_UP)
                                .doubleValue();
        }

        private List<CategoryBreakdown> getCategoryBreakdown(UUID accountBookId, LocalDate start, LocalDate end,
                        BigDecimal total) {
                List<CategorySummary> summaries = expenseRepository.sumByCategory(accountBookId, start, end);

                return summaries.stream()
                                .limit(5)
                                .map(s -> {
                                        double percentage = total.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                                                        : s.getAmount().multiply(BigDecimal.valueOf(100))
                                                                        .divide(total, 1, RoundingMode.HALF_UP) // 소수점 한
                                                                                                                // 자리까지
                                                                                                                // 계산
                                                                        .doubleValue();
                                        return CategoryBreakdown.builder()
                                                        .category(s.getName())
                                                        .amount(s.getAmount())
                                                        .percentage(percentage)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        private List<TopExpense> getTopExpenses(UUID accountBookId, LocalDate start, LocalDate end) {
                List<Expense> expenses = expenseRepository.findByAccountBookAndDateRange(
                                accountBookId, start, end, null);

                return expenses.stream()
                                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                                .limit(3)
                                .map(e -> TopExpense.builder()
                                                .merchant(e.getMerchant())
                                                .amount(e.getAmount())
                                                .date(e.getDate())
                                                .category(e.getCategory())
                                                .build())
                                .collect(Collectors.toList());
        }

        private TopMerchant getTopMerchant(UUID accountBookId, LocalDate start, LocalDate end) {
                List<Expense> expenses = expenseRepository.findByAccountBookAndDateRange(
                                accountBookId, start, end, null);

                Map<String, Long> merchantCount = expenses.stream()
                                .filter(e -> e.getMerchant() != null && !e.getMerchant().isBlank())
                                .collect(Collectors.groupingBy(Expense::getMerchant, Collectors.counting()));

                return merchantCount.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(entry -> TopMerchant.builder()
                                                .name(entry.getKey())
                                                .visitCount(entry.getValue().intValue())
                                                .build())
                                .orElse(null);
        }

        private BudgetSummary getBudgetSummary(UUID accountBookId, int year, int month, BigDecimal totalExpense) {
                Optional<Budget> budgetOpt = budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(
                                accountBookId, year, month);

                if (budgetOpt.isEmpty()) {
                        return null;
                }

                Budget budget = budgetOpt.get();
                int usagePercentage;
                if (budget.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                        usagePercentage = totalExpense.multiply(BigDecimal.valueOf(100))
                                        .divide(budget.getTargetAmount(), 0, RoundingMode.HALF_UP)
                                        .intValue();
                } else {
                        usagePercentage = totalExpense.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
                }

                return BudgetSummary.builder()
                                .targetAmount(budget.getTargetAmount())
                                .currentSpending(totalExpense)
                                .usagePercentage(usagePercentage)
                                .build();
        }
}
