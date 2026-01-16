package com.moneyflow.domain.statistics;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.dto.response.MonthlyStatisticsResponse;
import com.moneyflow.dto.response.MonthlyStatisticsResponse.CategoryBreakdown;
import com.moneyflow.dto.response.MonthlyStatisticsResponse.ComparisonData;
import com.moneyflow.dto.response.WeeklyStatisticsResponse;
import com.moneyflow.dto.response.WeeklyStatisticsResponse.DailyExpense;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
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
 * - 장부별 개별 통계 지원
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

        private final ExpenseRepository expenseRepository;
        private final AccountBookRepository accountBookRepository;

        /**
         * 월간 통계 조회
         *
         * @param userId        사용자 ID
         * @param year          년도
         * @param month         월 (1-12)
         * @param accountBookId 장부 ID (null이면 기본 장부)
         * @return 월간 통계 정보
         */
        public MonthlyStatisticsResponse getMonthlyStatistics(UUID userId, int year, int month, UUID accountBookId) {
                // 조회 기간 계산
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.plusMonths(1).minusDays(1);

                // 장부 확인 및 권한 검증
                AccountBook accountBook = getAndValidateAccountBook(userId, accountBookId);

                // 해당 월 지출 조회
                List<Expense> expenses = getExpensesForAccountBook(accountBook.getAccountBookId(), startDate, endDate);

                // 총 지출 금액 계산
                BigDecimal totalAmount = expenses.stream()
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 카테고리별 지출 집계
                Map<String, BigDecimal> categoryMap = expenses.stream()
                                .collect(Collectors.groupingBy(
                                                Expense::getCategory,
                                                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount,
                                                                BigDecimal::add)));

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
                List<Expense> lastMonthExpenses = getExpensesForAccountBook(accountBook.getAccountBookId(),
                                lastMonthStart, lastMonthEnd);

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
                                .accountBookId(accountBook.getAccountBookId())
                                .accountBookName(accountBook.getName())
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
         * @param userId        사용자 ID
         * @param startDate     시작 날짜 (해당 날짜부터 6일 후까지 총 7일)
         * @param accountBookId 장부 ID (null이면 기본 장부)
         * @return 주간 통계 정보
         */
        public WeeklyStatisticsResponse getWeeklyStatistics(UUID userId, LocalDate startDate, UUID accountBookId) {
                // 조회 기간 계산 (startDate부터 6일 후까지 총 7일)
                LocalDate endDate = startDate.plusDays(6);

                // 장부 확인 및 권한 검증
                AccountBook accountBook = getAndValidateAccountBook(userId, accountBookId);

                // 해당 기간 지출 조회
                List<Expense> expenses = getExpensesForAccountBook(accountBook.getAccountBookId(), startDate, endDate);

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
                                                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount,
                                                                BigDecimal::add)));

                // 최다 지출 카테고리 찾기 (금액 기준)
                String topCategory = categoryMap.isEmpty() ? null
                                : categoryMap.entrySet().stream()
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
                                .accountBookId(accountBook.getAccountBookId())
                                .accountBookName(accountBook.getName())
                                .dailyExpenses(dailyExpenses)
                                .topCategory(topCategory)
                                .averageDaily(averageDaily)
                                .build();
        }

        /**
         * 장부 조회 및 권한 검증
         * accountBookId가 null이면 기본 장부 반환
         */
        private AccountBook getAndValidateAccountBook(UUID userId, UUID accountBookId) {
                AccountBook accountBook;

                if (accountBookId != null) {
                        // 특정 장부 조회
                        accountBook = accountBookRepository.findById(accountBookId)
                                        .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

                        // 사용자가 해당 장부의 멤버인지 확인
                        boolean isMember = accountBook.getMembers().stream()
                                        .anyMatch(m -> m.getUser().getUserId().equals(userId));

                        if (!isMember) {
                                throw new UnauthorizedException("해당 장부에 접근할 권한이 없습니다");
                        }
                } else {
                        // 기본 장부 조회
                        accountBook = accountBookRepository.findDefaultAccountBookByUserId(userId)
                                        .orElseThrow(() -> new ResourceNotFoundException("기본 장부를 찾을 수 없습니다"));
                }

                return accountBook;
        }

        /**
         * 장부별 지출 내역 조회
         */
        private List<Expense> getExpensesForAccountBook(UUID accountBookId, LocalDate startDate, LocalDate endDate) {
                return expenseRepository.findByAccountBookAndDateRange(accountBookId, startDate, endDate, null);
        }

        /**
         * 카테고리별 지출 비율 계산
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
