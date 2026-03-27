package com.moneyflow.domain.budget;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.notification.Notification;
import com.moneyflow.domain.notification.NotificationRepository;
import com.moneyflow.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class BudgetAlertServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetAlertRepository budgetAlertRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AccountBookRepository accountBookRepository;

    @InjectMocks
    private BudgetAlertService budgetAlertService;

    // ──────────────────── 헬퍼 ────────────────────

    private User user(UUID userId) {
        return User.builder()
                .userId(userId)
                .email(userId + "@test.com")
                .nickname("테스터")
                .build();
    }

    private AccountBook accountBook(UUID accountBookId) {
        return AccountBook.builder()
                .accountBookId(accountBookId)
                .name("테스트 장부")
                .initialBalance(BigDecimal.ZERO)
                .build();
    }

    private Budget budget(UUID accountBookId, String targetAmount, int year, int month) {
        User user = user(UUID.randomUUID());
        AccountBook accountBook = accountBook(accountBookId);
        return Budget.builder()
                .budgetId(UUID.randomUUID())
                .user(user)
                .accountBook(accountBook)
                .year(year)
                .month(month)
                .targetAmount(new BigDecimal(targetAmount))
                .build();
    }

    // ──────────────────── 테스트 케이스 ────────────────────

    @Test
    @DisplayName("사용률 80% 미만: 알림을 생성하지 않는다")
    void checkBudget_whenUsageBelowEightyPercent_doesNotCreateAlert() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 15);
        Budget budget = budget(accountBookId, "100000", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));
        // 지출 79,999원 → 79.999% < 80%
        when(expenseRepository.sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)))
                .thenReturn(new BigDecimal("79999"));

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        verify(budgetAlertRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용률 80% 이상: WARNING_80 알림을 생성한다")
    void checkBudget_whenUsageReachesEightyPercent_createsWarning80Alert() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 15);
        Budget budget = budget(accountBookId, "100000", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));
        // 지출 80,000원 → 80% (경계값)
        when(expenseRepository.sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)))
                .thenReturn(new BigDecimal("80000"));
        // 아직 WARNING_80 알림 없음
        when(budgetAlertRepository.existsByAccountBookIdAndYearAndMonthAndThresholdType(
                accountBookId, 2026, 3, ThresholdType.WARNING_80))
                .thenReturn(false);

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        ArgumentCaptor<BudgetAlert> alertCaptor = ArgumentCaptor.forClass(BudgetAlert.class);
        verify(budgetAlertRepository).save(alertCaptor.capture());
        BudgetAlert saved = alertCaptor.getValue();
        assertThat(saved.getThresholdType()).isEqualTo(ThresholdType.WARNING_80);
        assertThat(saved.getAccountBookId()).isEqualTo(accountBookId);
        assertThat(saved.getYear()).isEqualTo(2026);
        assertThat(saved.getMonth()).isEqualTo(3);
    }

    @Test
    @DisplayName("사용률 100% 이상: EXCEEDED_100 알림을 생성한다")
    void checkBudget_whenUsageReachesHundredPercent_createsExceeded100Alert() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 20);
        Budget budget = budget(accountBookId, "100000", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));
        // 지출 100,000원 → 100%
        when(expenseRepository.sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)))
                .thenReturn(new BigDecimal("100000"));
        // EXCEEDED_100은 아직 없음
        when(budgetAlertRepository.existsByAccountBookIdAndYearAndMonthAndThresholdType(
                accountBookId, 2026, 3, ThresholdType.EXCEEDED_100))
                .thenReturn(false);

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        ArgumentCaptor<BudgetAlert> alertCaptor = ArgumentCaptor.forClass(BudgetAlert.class);
        verify(budgetAlertRepository).save(alertCaptor.capture());
        BudgetAlert saved = alertCaptor.getValue();
        assertThat(saved.getThresholdType()).isEqualTo(ThresholdType.EXCEEDED_100);
    }

    @Test
    @DisplayName("WARNING_80 알림이 이미 존재하면 중복 생성하지 않는다")
    void checkBudget_whenWarning80AlreadyExists_doesNotCreateDuplicate() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 15);
        Budget budget = budget(accountBookId, "100000", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));
        when(expenseRepository.sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)))
                .thenReturn(new BigDecimal("85000"));
        // 이미 WARNING_80 알림 존재
        when(budgetAlertRepository.existsByAccountBookIdAndYearAndMonthAndThresholdType(
                accountBookId, 2026, 3, ThresholdType.WARNING_80))
                .thenReturn(true);

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        verify(budgetAlertRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("예산 금액이 0인 경우: 알림을 생성하지 않는다")
    void checkBudget_whenTargetAmountIsZero_doesNotCreateAlert() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 15);
        Budget budget = budget(accountBookId, "0", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        verify(expenseRepository, never()).sumAmountByPeriod(any(), any(), any());
        verify(budgetAlertRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("예산 금액이 음수인 경우: 알림을 생성하지 않는다")
    void checkBudget_whenTargetAmountIsNegative_doesNotCreateAlert() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 15);
        Budget budget = budget(accountBookId, "-100", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        verify(expenseRepository, never()).sumAmountByPeriod(any(), any(), any());
        verify(budgetAlertRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("예산이 설정되지 않은 경우: 알림을 생성하지 않는다")
    void checkBudget_whenNoBudgetSet_doesNotCreateAlert() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 15);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.empty());

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        verify(expenseRepository, never()).sumAmountByPeriod(any(), any(), any());
        verify(budgetAlertRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("WARNING_80 알림 생성 시 Notification도 함께 저장한다")
    void checkBudget_whenWarning80Triggered_savesNotification() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 15);
        Budget budget = budget(accountBookId, "100000", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));
        when(expenseRepository.sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)))
                .thenReturn(new BigDecimal("80000"));
        when(budgetAlertRepository.existsByAccountBookIdAndYearAndMonthAndThresholdType(
                accountBookId, 2026, 3, ThresholdType.WARNING_80))
                .thenReturn(false);

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification notification = notifCaptor.getValue();
        assertThat(notification.getUser()).isEqualTo(budget.getUser());
        assertThat(notification.getTitle()).isNotBlank();
        assertThat(notification.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("EXCEEDED_100 알림 생성 시 Notification도 함께 저장한다")
    void checkBudget_whenExceeded100Triggered_savesNotification() {
        UUID accountBookId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2026, 3, 25);
        Budget budget = budget(accountBookId, "100000", 2026, 3);

        when(budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, 2026, 3))
                .thenReturn(Optional.of(budget));
        when(expenseRepository.sumAmountByPeriod(
                accountBookId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)))
                .thenReturn(new BigDecimal("120000"));
        when(budgetAlertRepository.existsByAccountBookIdAndYearAndMonthAndThresholdType(
                accountBookId, 2026, 3, ThresholdType.EXCEEDED_100))
                .thenReturn(false);

        budgetAlertService.checkBudgetAndAlert(accountBookId, expenseDate);

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification notification = notifCaptor.getValue();
        assertThat(notification.getUser()).isEqualTo(budget.getUser());
        assertThat(notification.getTitle()).isNotBlank();
        assertThat(notification.getMessage()).isNotBlank();
    }
}
