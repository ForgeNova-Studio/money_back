package com.moneyflow.domain.budget;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.notification.Notification;
import com.moneyflow.domain.notification.NotificationRepository;
import com.moneyflow.dto.response.BudgetAlertResponse;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertService {

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRepository budgetAlertRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationRepository notificationRepository;
    private final AccountBookRepository accountBookRepository;

    public List<BudgetAlertResponse> getAlerts(UUID userId, UUID accountBookId, int year, int month) {
        AccountBook accountBook = accountBookRepository.findById(accountBookId)
                .orElseThrow(() -> new ResourceNotFoundException("가계부를 찾을 수 없습니다"));
        if (!accountBook.isMember(userId)) {
            throw UnauthorizedException.accessDenied("해당 가계부에 접근할 권한이 없습니다");
        }

        return budgetAlertRepository.findByAccountBookIdAndYearAndMonth(accountBookId, year, month)
                .stream()
                .map(alert -> BudgetAlertResponse.builder()
                        .alertId(alert.getId())
                        .accountBookId(alert.getAccountBookId())
                        .year(alert.getYear())
                        .month(alert.getMonth())
                        .thresholdType(alert.getThresholdType())
                        .triggeredAt(alert.getTriggeredAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void checkBudgetAndAlert(UUID accountBookId, LocalDate expenseDate) {
        int year = expenseDate.getYear();
        int month = expenseDate.getMonthValue();

        budgetRepository
                .findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, year, month)
                .ifPresent(budget -> checkThresholds(budget, accountBookId, year, month, expenseDate));
    }

    private void checkThresholds(Budget budget, UUID accountBookId, int year, int month, LocalDate expenseDate) {
        BigDecimal targetAmount = budget.getTargetAmount();
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("예산 금액이 0 이하: accountBookId={}, targetAmount={}", accountBookId, targetAmount);
            return;
        }

        LocalDate firstDay = expenseDate.withDayOfMonth(1);
        LocalDate lastDay = expenseDate.withDayOfMonth(expenseDate.lengthOfMonth());

        BigDecimal totalExpenses = java.util.Optional
                .ofNullable(expenseRepository.sumAmountByPeriod(accountBookId, firstDay, lastDay))
                .orElse(BigDecimal.ZERO);

        if (isAtOrAboveThreshold(totalExpenses, targetAmount, 100)) {
            sendAlertIfNew(budget, accountBookId, year, month, ThresholdType.EXCEEDED_100,
                    "예산 초과 알림", month + "월 예산을 100% 초과하였습니다.");
            return;
        }

        if (isAtOrAboveThreshold(totalExpenses, targetAmount, 80)) {
            sendAlertIfNew(budget, accountBookId, year, month, ThresholdType.WARNING_80,
                    "예산 80% 도달 알림", month + "월 예산의 80%에 도달하였습니다.");
        }
    }

    // 반올림 오류 방지를 위해 나눗셈 대신 곱셈으로 비교
    // actual / target >= thresholdPercent / 100  →  actual * 100 >= target * thresholdPercent
    private boolean isAtOrAboveThreshold(BigDecimal actual, BigDecimal target, int thresholdPercent) {
        return actual.multiply(BigDecimal.valueOf(100))
                .compareTo(target.multiply(BigDecimal.valueOf(thresholdPercent))) >= 0;
    }

    private void sendAlertIfNew(Budget budget, UUID accountBookId, int year, int month,
                                ThresholdType thresholdType, String title, String message) {
        boolean alreadySent = budgetAlertRepository
                .existsByAccountBookIdAndYearAndMonthAndThresholdType(accountBookId, year, month, thresholdType);
        if (alreadySent) {
            return;
        }

        budgetAlertRepository.save(BudgetAlert.builder()
                .budget(budget)
                .accountBookId(accountBookId)
                .year(year)
                .month(month)
                .thresholdType(thresholdType)
                .build());

        notificationRepository.save(Notification.builder()
                .user(budget.getUser())
                .title(title)
                .message(message)
                .build());

        log.info("예산 알림 생성: accountBookId={}, year={}, month={}, type={}", accountBookId, year, month, thresholdType);
    }

    @Async
    @TransactionalEventListener
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        checkBudgetAndAlert(event.accountBookId(), event.expenseDate());
    }
}
