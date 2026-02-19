package com.moneyflow.domain.budget;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookMember;
import com.moneyflow.domain.accountbook.AccountBookMemberId;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.response.BudgetResponse;
import com.moneyflow.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountBookRepository accountBookRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    @DisplayName("예산 삭제: 생성자가 아니어도 가계부 멤버면 삭제 가능")
    void deleteBudget_allowsMemberEvenIfNotCreator() {
        UUID creatorId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();

        User creator = user(creatorId);
        AccountBook accountBook = accountBook(accountBookId, creatorId, memberId);
        Budget budget = budget(budgetId, creator, accountBook, 2026, 2, "1000000");

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(memberId, budgetId);

        verify(budgetRepository).delete(budget);
    }

    @Test
    @DisplayName("예산 삭제: 가계부 멤버가 아니면 UnauthorizedException")
    void deleteBudget_throwsWhenUserIsNotMember() {
        UUID creatorId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();

        User creator = user(creatorId);
        AccountBook accountBook = accountBook(accountBookId, creatorId);
        Budget budget = budget(budgetId, creator, accountBook, 2026, 2, "1000000");

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        assertThatThrownBy(() -> budgetService.deleteBudget(outsiderId, budgetId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("권한");

        verify(budgetRepository, never()).delete(budget);
    }

    @Test
    @DisplayName("예산 조회: 월 지출은 DB SUM 집계로 계산하고 응답 필드를 채운다")
    void getBudget_usesDbAggregateForCurrentSpending() {
        UUID memberId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();

        AccountBook accountBook = accountBook(accountBookId, memberId);
        User owner = user(memberId);
        Budget budget = budget(budgetId, owner, accountBook, 2026, 2, "1000000");

        when(accountBookRepository.findById(accountBookId)).thenReturn(Optional.of(accountBook));
        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 2))
                .thenReturn(Optional.of(budget));
        when(expenseRepository.sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28)))
                .thenReturn(new BigDecimal("250000"));

        BudgetResponse response = budgetService.getBudget(memberId, accountBookId, 2026, 2);

        assertThat(response).isNotNull();
        assertThat(response.getCurrentSpending()).isEqualByComparingTo("250000");
        assertThat(response.getRemainingAmount()).isEqualByComparingTo("750000");
        assertThat(response.getUsagePercentage()).isEqualTo(25.0);

        verify(expenseRepository).sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28));
        verify(expenseRepository, never()).findByAccountBookAndDateRange(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private User user(UUID userId) {
        return User.builder()
                .userId(userId)
                .email(userId + "@test.com")
                .nickname("tester")
                .build();
    }

    private AccountBook accountBook(UUID accountBookId, UUID... memberIds) {
        AccountBook accountBook = AccountBook.builder()
                .accountBookId(accountBookId)
                .name("테스트 장부")
                .members(new java.util.ArrayList<>())
                .initialBalance(BigDecimal.ZERO)
                .build();

        List<AccountBookMember> members = java.util.Arrays.stream(memberIds)
                .map(id -> AccountBookMember.builder()
                        .id(new AccountBookMemberId(accountBookId, id))
                        .accountBook(accountBook)
                        .user(user(id))
                        .build())
                .toList();

        accountBook.setMembers(members);
        return accountBook;
    }

    private Budget budget(
            UUID budgetId,
            User creator,
            AccountBook accountBook,
            int year,
            int month,
            String targetAmount) {
        return Budget.builder()
                .budgetId(budgetId)
                .user(creator)
                .accountBook(accountBook)
                .year(year)
                .month(month)
                .targetAmount(new BigDecimal(targetAmount))
                .build();
    }
}
