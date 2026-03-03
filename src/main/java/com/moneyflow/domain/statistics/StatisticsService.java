package com.moneyflow.domain.statistics;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.income.IncomeRepository;
import com.moneyflow.dto.projection.CategorySummary;
import com.moneyflow.dto.response.CategoryMonthlyComparisonResponse;
import com.moneyflow.dto.response.CategoryMonthlyComparisonResponse.CategoryComparison;
import com.moneyflow.dto.response.MonthlyStatisticsResponse;
import com.moneyflow.dto.response.MonthlyStatisticsResponse.CategoryBreakdown;
import com.moneyflow.dto.response.MonthlyStatisticsResponse.ComparisonData;
import com.moneyflow.dto.response.TotalAssetResponse;
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
 * - 자산 현황 조회 (총자산, 기간별 손익)
 * - 장부별 개별 통계 지원
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

        private final ExpenseRepository expenseRepository;
        private final IncomeRepository incomeRepository;
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
                UUID bookId = accountBook.getAccountBookId();

                // 총 지출 금액 계산 (DB SUM)
                BigDecimal totalAmount = expenseRepository.sumAmountByPeriod(bookId, startDate, endDate);

                // 카테고리별 지출 집계 (DB GROUP BY - 메모리 효율적)
                List<CategorySummary> categorySummaries = expenseRepository.sumByCategory(bookId, startDate, endDate);

                // CategoryBreakdown 변환 (이미 금액 기준 내림차순 정렬됨)
                List<CategoryBreakdown> categoryBreakdown = categorySummaries.stream()
                                .map(summary -> CategoryBreakdown.builder()
                                                .category(summary.getName())
                                                .amount(summary.getAmount())
                                                .percentage(calculatePercent(summary.getAmount(), totalAmount))
                                                .build())
                                .collect(Collectors.toList());

                // 전월 대비 증감 계산 (DB SUM)
                LocalDate lastMonthStart = startDate.minusMonths(1);
                LocalDate lastMonthEnd = lastMonthStart.plusMonths(1).minusDays(1);
                BigDecimal lastMonthTotal = expenseRepository.sumAmountByPeriod(bookId, lastMonthStart, lastMonthEnd);

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
         * 카테고리별 전월 대비 변화 조회
         *
         * @param userId        사용자 ID
         * @param year          년도
         * @param month         월 (1-12)
         * @param accountBookId 장부 ID (null이면 기본 장부)
         * @return 카테고리별 전월 대비 변화 정보
         */
        public CategoryMonthlyComparisonResponse getCategoryMonthlyComparison(UUID userId, int year, int month, UUID accountBookId) {
                // 장부 확인 및 권한 검증
                AccountBook accountBook = getAndValidateAccountBook(userId, accountBookId);
                UUID bookId = accountBook.getAccountBookId();

                // 이번 달 기간
                LocalDate currentStart = LocalDate.of(year, month, 1);
                LocalDate currentEnd = currentStart.plusMonths(1).minusDays(1);

                // 전월 기간
                LocalDate previousStart = currentStart.minusMonths(1);
                LocalDate previousEnd = previousStart.plusMonths(1).minusDays(1);

                // 이번 달 / 전월 카테고리별 집계 (DB GROUP BY)
                List<CategorySummary> currentSummaries = expenseRepository.sumByCategory(bookId, currentStart, currentEnd);
                List<CategorySummary> previousSummaries = expenseRepository.sumByCategory(bookId, previousStart, previousEnd);

                // 전월 데이터를 Map으로 변환 (O(1) lookup)
                Map<String, BigDecimal> previousMap = previousSummaries.stream()
                                .collect(Collectors.toMap(CategorySummary::getName, CategorySummary::getAmount));

                // 이번 달 데이터를 Map으로 변환 (전월에만 있는 카테고리 처리용)
                Set<String> currentCategories = currentSummaries.stream()
                                .map(CategorySummary::getName)
                                .collect(Collectors.toSet());

                // 카테고리별 비교 리스트 생성 (이번 달에 있는 카테고리)
                List<CategoryComparison> comparisons = new ArrayList<>();

                for (CategorySummary current : currentSummaries) {
                        BigDecimal currentAmount = current.getAmount();
                        BigDecimal previousAmount = previousMap.getOrDefault(current.getName(), BigDecimal.ZERO);
                        comparisons.add(buildCategoryComparison(current.getName(), currentAmount, previousAmount));
                }

                // 전월에만 있고 이번 달에 없는 카테고리 추가
                for (CategorySummary previous : previousSummaries) {
                        if (!currentCategories.contains(previous.getName())) {
                                comparisons.add(buildCategoryComparison(previous.getName(), BigDecimal.ZERO, previous.getAmount()));
                        }
                }

                // 이번 달 금액 기준 내림차순 정렬
                comparisons.sort((a, b) -> b.getCurrentAmount().compareTo(a.getCurrentAmount()));

                // 총 지출 계산
                BigDecimal currentTotal = expenseRepository.sumAmountByPeriod(bookId, currentStart, currentEnd);
                BigDecimal previousTotal = expenseRepository.sumAmountByPeriod(bookId, previousStart, previousEnd);

                return CategoryMonthlyComparisonResponse.builder()
                                .accountBookId(bookId)
                                .accountBookName(accountBook.getName())
                                .year(year)
                                .month(month)
                                .currentMonthTotal(currentTotal)
                                .previousMonthTotal(previousTotal)
                                .categories(comparisons)
                                .build();
        }

        /**
         * 카테고리별 비교 데이터 생성
         */
        private CategoryComparison buildCategoryComparison(String category, BigDecimal currentAmount, BigDecimal previousAmount) {
                BigDecimal diff = currentAmount.subtract(previousAmount);
                Double diffPercentage = null;
                if (previousAmount.compareTo(BigDecimal.ZERO) != 0) {
                        diffPercentage = diff.divide(previousAmount, 4, RoundingMode.HALF_UP)
                                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                }
                return CategoryComparison.builder()
                                .category(category)
                                .currentAmount(currentAmount)
                                .previousAmount(previousAmount)
                                .diff(diff)
                                .diffPercentage(diffPercentage)
                                .build();
        }

        /**
         * 장부 조회 및 권한 검증 (N+1 방지: Members JOIN FETCH)
         * accountBookId가 null이면 기본 장부 반환
         */
        private AccountBook getAndValidateAccountBook(UUID userId, UUID accountBookId) {
                AccountBook accountBook;

                if (accountBookId != null) {
                        // 특정 장부 조회 (JOIN FETCH로 Members + User 미리 로드 - N+1 방지)
                        accountBook = accountBookRepository.findByIdWithMembersAndUsers(accountBookId)
                                        .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

                        // 사용자가 해당 장부의 멤버인지 확인 (이미 JOIN FETCH로 로드됨 - 추가 쿼리 없음)
                        boolean isMember = accountBook.getMembers().stream()
                                        .anyMatch(m -> m.getUser().getUserId().equals(userId));

                        if (!isMember) {
                                throw UnauthorizedException.accessDenied("해당 장부에 접근할 권한이 없습니다");
                        }
                } else {
                        // 기본 장부 조회 (이미 JOIN FETCH 포함)
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
         * 비율 계산 (통합 버전)
         *
         * @param amount 부분 금액
         * @param total  전체 금액
         * @return 비율 (0.0 ~ 100.0)
         */
        private double calculatePercent(BigDecimal amount, BigDecimal total) {
                if (total.compareTo(BigDecimal.ZERO) == 0) {
                        return 0.0;
                }
                return amount.divide(total, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue();
        }

        /**
         * 자산 현황 조회
         *
         * @param userId        사용자 ID
         * @param accountBookId 장부 ID (null이면 기본 장부)
         * @param startDate     기간 시작일 (null이면 이번 달 1일)
         * @param endDate       기간 종료일 (null이면 오늘)
         * @param includeStats  카테고리별 통계 포함 여부
         * @return 자산 현황 정보
         */
        public TotalAssetResponse getAssetStatistics(UUID userId, UUID accountBookId,
                        LocalDate startDate, LocalDate endDate, boolean includeStats) {

                // 1. 장부 조회 및 권한 검증
                AccountBook accountBook = getAndValidateAccountBook(userId, accountBookId);
                UUID bookId = accountBook.getAccountBookId();

                // 2. [현재 자산 계산] 날짜 상관없이 전체 누적 데이터
                BigDecimal initialBalance = accountBook.getInitialBalance();
                BigDecimal totalIncome = incomeRepository.sumTotalAmount(bookId);
                BigDecimal totalExpense = expenseRepository.sumTotalAmount(bookId);
                BigDecimal currentAssets = initialBalance.add(totalIncome).subtract(totalExpense);

                // 3. [기간 손익 계산] 필터 기간 내 데이터
                BigDecimal periodIncome = incomeRepository.sumAmountByPeriod(bookId, startDate, endDate);
                BigDecimal periodExpense = expenseRepository.sumAmountByPeriod(bookId, startDate, endDate);
                BigDecimal periodNetIncome = periodIncome.subtract(periodExpense);

                // 4. [카테고리별 통계 계산] (선택적)
                List<TotalAssetResponse.CategoryStat> incomeStats = null;
                List<TotalAssetResponse.CategoryStat> expenseStats = null;

                if (includeStats) {
                        // 4-1. 수입 출처별 통계
                        incomeStats = calculateIncomeStats(bookId, startDate, endDate, periodIncome);

                        // 4-2. 지출 카테고리별 통계
                        expenseStats = calculateExpenseStats(bookId, startDate, endDate, periodExpense);
                }

                // 5. 응답 생성
                return TotalAssetResponse.builder()
                                .accountBookId(bookId)
                                .accountBookName(accountBook.getName())
                                // 현재 자산 상태
                                .currentTotalAssets(currentAssets)
                                .initialBalance(initialBalance)
                                .totalIncome(totalIncome)
                                .totalExpense(totalExpense)
                                // 기간별 손익
                                .filterStartDate(startDate)
                                .filterEndDate(endDate)
                                .periodIncome(periodIncome)
                                .periodExpense(periodExpense)
                                .periodNetIncome(periodNetIncome)
                                // 카테고리별 통계 (선택적)
                                .incomeStats(incomeStats)
                                .expenseStats(expenseStats)
                                .build();
        }

        /**
         * 수입 출처별 통계 계산 (DB GROUP BY 사용)
         *
         * @param bookId      장부 ID
         * @param startDate   시작일
         * @param endDate     종료일
         * @param totalAmount 총 수입 금액
         * @return 수입 출처별 통계 리스트 (금액 기준 내림차순)
         */
        private List<TotalAssetResponse.CategoryStat> calculateIncomeStats(
                        UUID bookId, LocalDate startDate, LocalDate endDate, BigDecimal totalAmount) {

                // DB에서 직접 GROUP BY 집계 (메모리 효율적)
                List<CategorySummary> summaries = incomeRepository.sumBySource(bookId, startDate, endDate);

                // CategoryStat 변환 (이미 금액 기준 내림차순 정렬됨)
                return summaries.stream()
                                .map(summary -> TotalAssetResponse.CategoryStat.builder()
                                                .name(summary.getName())
                                                .amount(summary.getAmount())
                                                .percent(calculatePercent(summary.getAmount(), totalAmount))
                                                .build())
                                .collect(Collectors.toList());
        }

        /**
         * 지출 카테고리별 통계 계산 (DB GROUP BY 사용)
         *
         * @param bookId      장부 ID
         * @param startDate   시작일
         * @param endDate     종료일
         * @param totalAmount 총 지출 금액
         * @return 지출 카테고리별 통계 리스트 (금액 기준 내림차순)
         */
        private List<TotalAssetResponse.CategoryStat> calculateExpenseStats(
                        UUID bookId, LocalDate startDate, LocalDate endDate, BigDecimal totalAmount) {

                // DB에서 직접 GROUP BY 집계 (메모리 효율적)
                List<CategorySummary> summaries = expenseRepository.sumByCategory(bookId, startDate, endDate);

                // CategoryStat 변환 (이미 금액 기준 내림차순 정렬됨)
                return summaries.stream()
                                .map(summary -> TotalAssetResponse.CategoryStat.builder()
                                                .name(summary.getName())
                                                .amount(summary.getAmount())
                                                .percent(calculatePercent(summary.getAmount(), totalAmount))
                                                .build())
                                .collect(Collectors.toList());
        }

}
