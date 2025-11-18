package com.moneyflow.domain.statistics;

import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.dto.response.MonthlyStatisticsResponse;
import com.moneyflow.dto.response.MonthlyStatisticsResponse.CategoryBreakdown;
import com.moneyflow.dto.response.MonthlyStatisticsResponse.ComparisonData;
import com.moneyflow.dto.response.WeeklyStatisticsResponse;
import com.moneyflow.dto.response.WeeklyStatisticsResponse.DailyExpense;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 통계 서비스
 *
 * 기능:
 * - 월간 통계 조회 (카테고리별 지출, 전월 대비)
 * - 주간 통계 조회 (일별 지출, 최다 카테고리, 일평균)
 * - 개인/커플 지출 통합 처리
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final ExpenseRepository expenseRepository;

    /**
     * 월간 통계 조회
     *
     * @param userId 사용자 ID
     * @param year 년도
     * @param month 월 (1-12)
     * @return 월간 통계 정보
     */
    public MonthlyStatisticsResponse getMonthlyStatistics(UUID userId, int year, int month) {
        // 조회 기간 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 해당 월 지출 조회 (개인 + 커플 지출 포함)
        List<Expense> expenses = getExpensesForUser(userId, startDate, endDate);

        // 총 지출 금액 계산
        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 카테고리별 지출 집계
        Map<String, BigDecimal> categoryMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // 카테고리별 내역 생성 (금액 기준 내림차순 정렬)
        List<CategoryBreakdown> categoryBreakdown = categoryMap.entrySet().stream()
                .map(entry -> CategoryBreakdown.builder()
                        .category(entry.getKey())
                        .amount(entry.getValue())
                        .percentage(calculatePercentage(entry.getValue(), totalAmount))
                        .build())
                .sorted(Comparator.comparing(CategoryBreakdown::getAmount).reversed())
                .collect(Collectors.toList());

        // 전월 대비 증감 계산
        LocalDate lastMonthStart = startDate.minusMonths(1);
        LocalDate lastMonthEnd = lastMonthStart.plusMonths(1).minusDays(1);
        List<Expense> lastMonthExpenses = getExpensesForUser(userId, lastMonthStart, lastMonthEnd);

        BigDecimal lastMonthTotal = lastMonthExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal diff = totalAmount.subtract(lastMonthTotal);
        double diffPercentage = lastMonthTotal.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : diff.divide(lastMonthTotal, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();

        // 응답 생성
        return MonthlyStatisticsResponse.builder()
                .totalAmount(totalAmount)
                .categoryBreakdown(categoryBreakdown)
                .comparisonWithLastMonth(ComparisonData.builder()
                        .diff(diff)
                        .diffPercentage(diffPercentage)
                        .build())
                .build();
    }

    /**
     * 주간 통계 조회
     *
     * @param userId 사용자 ID
     * @param startDate 시작 날짜 (해당 날짜부터 6일 후까지 총 7일)
     * @return 주간 통계 정보
     */
    public WeeklyStatisticsResponse getWeeklyStatistics(UUID userId, LocalDate startDate) {
        // 조회 기간 계산 (startDate부터 6일 후까지 총 7일)
        LocalDate endDate = startDate.plusDays(6);

        // 해당 기간 지출 조회 (개인 + 커플 지출 포함)
        List<Expense> expenses = getExpensesForUser(userId, startDate, endDate);

        // 일별 지출 집계
        Map<LocalDate, BigDecimal> dailyMap = new LinkedHashMap<>();

        // 모든 날짜를 0원으로 초기화 (지출이 없는 날도 표시하기 위함)
        for (int i = 0; i < 7; i++) {
            dailyMap.put(startDate.plusDays(i), BigDecimal.ZERO);
        }

        // 실제 지출 데이터로 업데이트
        expenses.forEach(expense -> {
            LocalDate date = expense.getDate();
            dailyMap.put(date, dailyMap.get(date).add(expense.getAmount()));
        });

        // 일별 지출 리스트 생성 (날짜 순서대로)
        List<DailyExpense> dailyExpenses = dailyMap.entrySet().stream()
                .map(entry -> DailyExpense.builder()
                        .date(entry.getKey())
                        .amount(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // 카테고리별 지출 집계 (최다 지출 카테고리 찾기)
        Map<String, BigDecimal> categoryMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // 최다 지출 카테고리 찾기 (금액 기준)
        String topCategory = categoryMap.isEmpty() ? null : categoryMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 총 지출 금액 계산
        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 일평균 지출 계산 (총 지출 / 7일)
        BigDecimal averageDaily = totalAmount.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

        // 응답 생성
        return WeeklyStatisticsResponse.builder()
                .dailyExpenses(dailyExpenses)
                .topCategory(topCategory)
                .averageDaily(averageDaily)
                .build();
    }

    /**
     * 사용자의 지출 내역 조회 (개인 지출만)
     *
     * TODO: 커플 모드 구현 시 커플 지출도 포함하도록 수정
     *
     * @param userId 사용자 ID
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 지출 목록
     */
    private List<Expense> getExpensesForUser(UUID userId, LocalDate startDate, LocalDate endDate) {
        // 현재는 개인 지출만 조회 (카테고리 필터 없이 전체 조회)
        return expenseRepository.findExpensesByUserAndDateRange(userId, startDate, endDate, null);

        // TODO: Phase 2에서 커플 모드 구현 시 아래 로직 추가
        // Optional<Couple> couple = coupleRepository.findByUser1IdOrUser2Id(userId, userId);
        // if (couple.isPresent() && couple.get().getLinkedAt() != null) {
        //     List<Expense> coupleExpenses = expenseRepository.findExpensesByCoupleAndDateRange(
        //             couple.get().getCoupleId(), startDate, endDate, null);
        //     personalExpenses.addAll(coupleExpenses);
        // }
    }

    /**
     * 카테고리별 지출 비율 계산
     *
     * @param amount 카테고리 지출 금액
     * @param total 총 지출 금액
     * @return 비율 (0-100)
     */
    private int calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return amount.divide(total, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();
    }
}
