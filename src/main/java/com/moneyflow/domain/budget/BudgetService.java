package com.moneyflow.domain.budget;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.BudgetRequest;
import com.moneyflow.dto.response.BudgetResponse;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * 예산 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final AccountBookRepository accountBookRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * 예산 생성 또는 수정
     * 같은 년월에 이미 예산이 있으면 업데이트, 없으면 새로 생성
     */
    @Transactional
    public BudgetResponse createOrUpdateBudget(UUID userId, BudgetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        // 가계부 조회 및 권한 검증
        AccountBook accountBook = accountBookRepository.findById(request.getAccountBookId())
                .orElseThrow(() -> new ResourceNotFoundException("가계부를 찾을 수 없습니다"));

        if (!accountBook.isMember(userId)) {
            throw new UnauthorizedException("해당 가계부에 접근할 권한이 없습니다");
        }

        // 기존 예산이 있는지 확인
        Budget budget = budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(
                        request.getAccountBookId(), request.getYear(), request.getMonth())
                .orElse(null);

        if (budget != null) {
            // 기존 예산 업데이트
            budget.setTargetAmount(request.getTargetAmount());
            log.info("Updated budget for account book {} - {}/{}", request.getAccountBookId(), request.getYear(), request.getMonth());
        } else {
            // 새 예산 생성
            budget = Budget.builder()
                    .user(user)
                    .accountBook(accountBook)
                    .year(request.getYear())
                    .month(request.getMonth())
                    .targetAmount(request.getTargetAmount())
                    .build();
            log.info("Created budget for account book {} - {}/{}", request.getAccountBookId(), request.getYear(), request.getMonth());
        }

        Budget savedBudget = budgetRepository.save(budget);
        return toResponse(savedBudget);
    }

    /**
     * 특정 년월의 예산 조회
     * 예산이 없으면 null 반환 (예산이 없는 것은 정상 상태)
     */
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID userId, UUID accountBookId, Integer year, Integer month) {
        // 가계부 권한 검증
        AccountBook accountBook = accountBookRepository.findById(accountBookId)
                .orElseThrow(() -> new ResourceNotFoundException("가계부를 찾을 수 없습니다"));

        if (!accountBook.isMember(userId)) {
            throw new UnauthorizedException("해당 가계부에 접근할 권한이 없습니다");
        }

        Budget budget = budgetRepository.findByAccountBookAccountBookIdAndYearAndMonth(accountBookId, year, month)
                .orElse(null);

        if (budget == null) {
            return null;
        }

        return toResponse(budget);
    }

    /**
     * 예산 삭제
     */
    @Transactional
    public void deleteBudget(UUID userId, UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("예산을 찾을 수 없습니다"));

        if (!budget.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("해당 예산을 삭제할 권한이 없습니다");
        }

        budgetRepository.delete(budget);
        log.info("Deleted budget: {}", budgetId);
    }

    /**
     * Entity를 Response DTO로 변환 (현재 소비 금액 포함)
     */
    private BudgetResponse toResponse(Budget budget) {
        // 해당 월의 시작일과 종료일 계산
        YearMonth yearMonth = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 해당 가계부의 해당 월 총 지출 계산
        List<Expense> expenses = expenseRepository.findByAccountBookAndDateRange(
                budget.getAccountBook().getAccountBookId(), startDate, endDate, null);

        BigDecimal currentSpending = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 남은 금액 계산
        BigDecimal remainingAmount = budget.getTargetAmount().subtract(currentSpending);

        // 사용률 계산 (%)
        Double usagePercentage = 0.0;
        if (budget.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            usagePercentage = currentSpending
                    .divide(budget.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return BudgetResponse.builder()
                .budgetId(budget.getBudgetId())
                .userId(budget.getUser().getUserId())
                .accountBookId(budget.getAccountBook().getAccountBookId())
                .year(budget.getYear())
                .month(budget.getMonth())
                .targetAmount(budget.getTargetAmount())
                .currentSpending(currentSpending)
                .remainingAmount(remainingAmount)
                .usagePercentage(usagePercentage)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
}
