package com.moneyflow.domain.statistics;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.accountbook.AccountBookMember;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.income.IncomeRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.dto.projection.CategorySummary;
import com.moneyflow.dto.response.CategoryMonthlyComparisonResponse;
import com.moneyflow.dto.response.CategoryMonthlyComparisonResponse.CategoryComparison;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private AccountBookRepository accountBookRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private UUID userId;
    private UUID accountBookId;
    private AccountBook accountBook;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountBookId = UUID.randomUUID();

        // AccountBook 설정 (members 포함)
        User user = User.builder()
                .userId(userId)
                .email("test@naver.com")
                .nickname("테스트")
                .build();

        AccountBookMember member = AccountBookMember.builder()
                .user(user)
                .build();

        accountBook = AccountBook.builder()
                .accountBookId(accountBookId)
                .name("테스트 장부")
                .members(List.of(member))
                .initialBalance(BigDecimal.ZERO)
                .build();
    }

    private void setupAccountBookMock() {
        when(accountBookRepository.findByIdWithMembersAndUsers(accountBookId))
                .thenReturn(Optional.of(accountBook));
    }

    private CategorySummary mockCategorySummary(String name, BigDecimal amount) {
        return new CategorySummary() {
            @Override
            public String getName() { return name; }
            @Override
            public BigDecimal getAmount() { return amount; }
        };
    }

    @Nested
    @DisplayName("카테고리별 전월 대비 변화 조회")
    class GetCategoryMonthlyComparison {

        @Test
        @DisplayName("이번 달과 전월 모두 데이터가 있는 경우 - 카테고리별 증감 계산")
        void bothMonthsHaveData() {
            setupAccountBookMock();

            // 이번 달 (2026년 2월)
            List<CategorySummary> currentSummaries = List.of(
                    mockCategorySummary("FOOD", new BigDecimal("300000")),
                    mockCategorySummary("TRANSPORT", new BigDecimal("100000")),
                    mockCategorySummary("SHOPPING", new BigDecimal("50000"))
            );

            // 전월 (2026년 1월)
            List<CategorySummary> previousSummaries = List.of(
                    mockCategorySummary("FOOD", new BigDecimal("250000")),
                    mockCategorySummary("TRANSPORT", new BigDecimal("150000"))
            );

            LocalDate currentStart = LocalDate.of(2026, 2, 1);
            LocalDate currentEnd = LocalDate.of(2026, 2, 28);
            LocalDate previousStart = LocalDate.of(2026, 1, 1);
            LocalDate previousEnd = LocalDate.of(2026, 1, 31);

            when(expenseRepository.sumByCategory(accountBookId, currentStart, currentEnd))
                    .thenReturn(currentSummaries);
            when(expenseRepository.sumByCategory(accountBookId, previousStart, previousEnd))
                    .thenReturn(previousSummaries);
            when(expenseRepository.sumAmountByPeriod(accountBookId, currentStart, currentEnd))
                    .thenReturn(new BigDecimal("450000"));
            when(expenseRepository.sumAmountByPeriod(accountBookId, previousStart, previousEnd))
                    .thenReturn(new BigDecimal("400000"));

            CategoryMonthlyComparisonResponse response =
                    statisticsService.getCategoryMonthlyComparison(userId, 2026, 2, accountBookId);

            assertThat(response.getYear()).isEqualTo(2026);
            assertThat(response.getMonth()).isEqualTo(2);
            assertThat(response.getCurrentMonthTotal()).isEqualByComparingTo("450000");
            assertThat(response.getPreviousMonthTotal()).isEqualByComparingTo("400000");
            assertThat(response.getCategories()).hasSize(3);

            // 이번 달 금액 기준 내림차순 정렬 확인
            List<CategoryComparison> categories = response.getCategories();
            assertThat(categories.get(0).getCategory()).isEqualTo("FOOD");
            assertThat(categories.get(1).getCategory()).isEqualTo("TRANSPORT");
            assertThat(categories.get(2).getCategory()).isEqualTo("SHOPPING");

            // FOOD: 300000 - 250000 = +50000, +20%
            CategoryComparison food = categories.get(0);
            assertThat(food.getCurrentAmount()).isEqualByComparingTo("300000");
            assertThat(food.getPreviousAmount()).isEqualByComparingTo("250000");
            assertThat(food.getDiff()).isEqualByComparingTo("50000");
            assertThat(food.getDiffPercentage()).isEqualTo(20.0);

            // TRANSPORT: 100000 - 150000 = -50000, -33.33%
            CategoryComparison transport = categories.get(1);
            assertThat(transport.getCurrentAmount()).isEqualByComparingTo("100000");
            assertThat(transport.getPreviousAmount()).isEqualByComparingTo("150000");
            assertThat(transport.getDiff()).isEqualByComparingTo("-50000");
            assertThat(transport.getDiffPercentage()).isCloseTo(-33.33, org.assertj.core.data.Offset.offset(0.01));

            // SHOPPING: 50000 - 0 = +50000, 전월 0이므로 diffPercentage = null
            CategoryComparison shopping = categories.get(2);
            assertThat(shopping.getCurrentAmount()).isEqualByComparingTo("50000");
            assertThat(shopping.getPreviousAmount()).isEqualByComparingTo("0");
            assertThat(shopping.getDiff()).isEqualByComparingTo("50000");
            assertThat(shopping.getDiffPercentage()).isNull();
        }

        @Test
        @DisplayName("전월에만 있는 카테고리 - 이번 달 0원으로 표시")
        void previousOnlyCategory() {
            setupAccountBookMock();

            LocalDate currentStart = LocalDate.of(2026, 2, 1);
            LocalDate currentEnd = LocalDate.of(2026, 2, 28);
            LocalDate previousStart = LocalDate.of(2026, 1, 1);
            LocalDate previousEnd = LocalDate.of(2026, 1, 31);

            // 이번 달: FOOD만
            when(expenseRepository.sumByCategory(accountBookId, currentStart, currentEnd))
                    .thenReturn(List.of(mockCategorySummary("FOOD", new BigDecimal("100000"))));

            // 전월: FOOD + CULTURE
            when(expenseRepository.sumByCategory(accountBookId, previousStart, previousEnd))
                    .thenReturn(List.of(
                            mockCategorySummary("FOOD", new BigDecimal("200000")),
                            mockCategorySummary("CULTURE", new BigDecimal("80000"))
                    ));

            when(expenseRepository.sumAmountByPeriod(accountBookId, currentStart, currentEnd))
                    .thenReturn(new BigDecimal("100000"));
            when(expenseRepository.sumAmountByPeriod(accountBookId, previousStart, previousEnd))
                    .thenReturn(new BigDecimal("280000"));

            CategoryMonthlyComparisonResponse response =
                    statisticsService.getCategoryMonthlyComparison(userId, 2026, 2, accountBookId);

            assertThat(response.getCategories()).hasSize(2);

            // FOOD가 이번 달 금액 기준으로 먼저 나와야 함
            CategoryComparison food = response.getCategories().get(0);
            assertThat(food.getCategory()).isEqualTo("FOOD");
            assertThat(food.getCurrentAmount()).isEqualByComparingTo("100000");
            assertThat(food.getPreviousAmount()).isEqualByComparingTo("200000");

            // CULTURE: 이번 달 0, 전월 80000
            CategoryComparison culture = response.getCategories().get(1);
            assertThat(culture.getCategory()).isEqualTo("CULTURE");
            assertThat(culture.getCurrentAmount()).isEqualByComparingTo("0");
            assertThat(culture.getPreviousAmount()).isEqualByComparingTo("80000");
            assertThat(culture.getDiff()).isEqualByComparingTo("-80000");
            assertThat(culture.getDiffPercentage()).isEqualTo(-100.0);
        }

        @Test
        @DisplayName("전월 데이터가 없는 경우 - 모든 카테고리의 diffPercentage가 null")
        void noPreviousMonthData() {
            setupAccountBookMock();

            LocalDate currentStart = LocalDate.of(2026, 1, 1);
            LocalDate currentEnd = LocalDate.of(2026, 1, 31);
            LocalDate previousStart = LocalDate.of(2025, 12, 1);
            LocalDate previousEnd = LocalDate.of(2025, 12, 31);

            when(expenseRepository.sumByCategory(accountBookId, currentStart, currentEnd))
                    .thenReturn(List.of(
                            mockCategorySummary("FOOD", new BigDecimal("200000")),
                            mockCategorySummary("TRANSPORT", new BigDecimal("50000"))
                    ));
            when(expenseRepository.sumByCategory(accountBookId, previousStart, previousEnd))
                    .thenReturn(Collections.emptyList());
            when(expenseRepository.sumAmountByPeriod(accountBookId, currentStart, currentEnd))
                    .thenReturn(new BigDecimal("250000"));
            when(expenseRepository.sumAmountByPeriod(accountBookId, previousStart, previousEnd))
                    .thenReturn(BigDecimal.ZERO);

            CategoryMonthlyComparisonResponse response =
                    statisticsService.getCategoryMonthlyComparison(userId, 2026, 1, accountBookId);

            assertThat(response.getPreviousMonthTotal()).isEqualByComparingTo("0");
            assertThat(response.getCategories()).hasSize(2);

            for (CategoryComparison cat : response.getCategories()) {
                assertThat(cat.getPreviousAmount()).isEqualByComparingTo("0");
                assertThat(cat.getDiffPercentage()).isNull();
            }
        }

        @Test
        @DisplayName("이번 달과 전월 모두 데이터가 없는 경우 - 빈 리스트 반환")
        void noDataAtAll() {
            setupAccountBookMock();

            LocalDate currentStart = LocalDate.of(2026, 3, 1);
            LocalDate currentEnd = LocalDate.of(2026, 3, 31);
            LocalDate previousStart = LocalDate.of(2026, 2, 1);
            LocalDate previousEnd = LocalDate.of(2026, 2, 28);

            when(expenseRepository.sumByCategory(accountBookId, currentStart, currentEnd))
                    .thenReturn(Collections.emptyList());
            when(expenseRepository.sumByCategory(accountBookId, previousStart, previousEnd))
                    .thenReturn(Collections.emptyList());
            when(expenseRepository.sumAmountByPeriod(accountBookId, currentStart, currentEnd))
                    .thenReturn(BigDecimal.ZERO);
            when(expenseRepository.sumAmountByPeriod(accountBookId, previousStart, previousEnd))
                    .thenReturn(BigDecimal.ZERO);

            CategoryMonthlyComparisonResponse response =
                    statisticsService.getCategoryMonthlyComparison(userId, 2026, 3, accountBookId);

            assertThat(response.getCurrentMonthTotal()).isEqualByComparingTo("0");
            assertThat(response.getPreviousMonthTotal()).isEqualByComparingTo("0");
            assertThat(response.getCategories()).isEmpty();
        }

        @Test
        @DisplayName("기본 장부 사용 - accountBookId가 null인 경우")
        void defaultAccountBook() {
            when(accountBookRepository.findDefaultAccountBookByUserId(userId))
                    .thenReturn(Optional.of(accountBook));

            LocalDate currentStart = LocalDate.of(2026, 2, 1);
            LocalDate currentEnd = LocalDate.of(2026, 2, 28);
            LocalDate previousStart = LocalDate.of(2026, 1, 1);
            LocalDate previousEnd = LocalDate.of(2026, 1, 31);

            when(expenseRepository.sumByCategory(accountBookId, currentStart, currentEnd))
                    .thenReturn(Collections.emptyList());
            when(expenseRepository.sumByCategory(accountBookId, previousStart, previousEnd))
                    .thenReturn(Collections.emptyList());
            when(expenseRepository.sumAmountByPeriod(accountBookId, currentStart, currentEnd))
                    .thenReturn(BigDecimal.ZERO);
            when(expenseRepository.sumAmountByPeriod(accountBookId, previousStart, previousEnd))
                    .thenReturn(BigDecimal.ZERO);

            CategoryMonthlyComparisonResponse response =
                    statisticsService.getCategoryMonthlyComparison(userId, 2026, 2, null);

            assertThat(response.getAccountBookId()).isEqualTo(accountBookId);
            assertThat(response.getAccountBookName()).isEqualTo("테스트 장부");
        }
    }
}
