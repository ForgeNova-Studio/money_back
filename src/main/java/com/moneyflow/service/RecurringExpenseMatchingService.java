package com.moneyflow.service;

import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.recurringexpense.*;
import com.moneyflow.dto.response.MatchCandidateResponse;
import com.moneyflow.dto.response.RecurringExpensePaymentResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 고정비 매칭 서비스
 *
 * 실제 지출과 고정비(RecurringExpense)를 매칭하고 확정하는 로직을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseMatchingService {

    private final RecurringExpensePaymentRepository paymentRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;

    private static final int DEFAULT_MATCH_WINDOW_DAYS = 5;
    private static final int DEFAULT_AMOUNT_TOLERANCE_PERCENT = 20;

    /**
     * 월별 결제 현황 조회 (PENDING 자동 생성 포함)
     */
    @Transactional
    public List<RecurringExpensePaymentResponse> getPaymentsForMonth(UUID userId, int year, int month) {
        // 1. 먼저 PENDING 결제가 없는 고정비에 대해 자동 생성
        ensurePendingPaymentsExist(userId, year, month);

        // 2. 해당 월의 모든 결제 조회
        List<RecurringExpensePayment> payments = paymentRepository.findByUserAndPeriod(userId, year, month);

        return payments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * PENDING 결제 자동 생성 (조회 시 지연 생성)
     */
    @Transactional
    public void ensurePendingPaymentsExist(UUID userId, int year, int month) {
        // 활성 고정비 목록 조회
        List<RecurringExpense> activeExpenses = recurringExpenseRepository.findActiveRecurringExpenses(userId);

        for (RecurringExpense re : activeExpenses) {
            // 해당 월에 이미 결제가 있는지 확인
            boolean exists = paymentRepository.existsByRecurringExpense_RecurringExpenseIdAndPeriodYearAndPeriodMonth(
                    re.getRecurringExpenseId(), year, month);

            if (!exists) {
                // 예상 결제일 계산 (dayOfMonth 기준)
                LocalDate expectedDate = calculateExpectedDate(re, year, month);

                // PENDING 결제 생성
                RecurringExpensePayment payment = RecurringExpensePayment.builder()
                        .recurringExpense(re)
                        .periodYear(year)
                        .periodMonth(month)
                        .expectedAmount(re.getAmount())
                        .expectedDate(expectedDate)
                        .status(PaymentStatus.PENDING)
                        .build();

                paymentRepository.save(payment);
                log.info("Created PENDING payment for recurring expense: {} ({}년 {}월)",
                        re.getName(), year, month);
            }
        }
    }

    /**
     * 지출에 대한 매칭 후보 조회
     */
    @Transactional(readOnly = true)
    public List<MatchCandidateResponse> findMatchCandidates(UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("지출을 찾을 수 없습니다"));

        // 이미 연결된 경우 빈 목록 반환
        if (expense.getLinkedPaymentId() != null) {
            return List.of();
        }

        // 장부 기반 매칭 (장부가 없으면 빈 목록)
        if (expense.getAccountBook() == null) {
            return List.of();
        }

        UUID accountBookId = expense.getAccountBook().getAccountBookId();
        int year = expense.getDate().getYear();
        int month = expense.getDate().getMonthValue();

        // 해당 장부의 PENDING 결제 조회
        List<RecurringExpensePayment> pendingPayments = paymentRepository
                .findPendingByAccountBookAndPeriod(accountBookId, year, month);

        // 매칭 점수 계산 및 필터링
        List<MatchCandidateResponse> candidates = new ArrayList<>();

        for (RecurringExpensePayment payment : pendingPayments) {
            double matchScore = calculateMatchScore(expense, payment);

            if (matchScore > 0) {
                long daysDiff = Math.abs(ChronoUnit.DAYS.between(
                        expense.getDate(), payment.getExpectedDate()));

                double amountDiffPercent = calculateAmountDifferencePercent(
                        expense.getAmount(), payment.getExpectedAmount());

                candidates.add(MatchCandidateResponse.builder()
                        .paymentId(payment.getPaymentId())
                        .recurringExpenseId(payment.getRecurringExpense().getRecurringExpenseId())
                        .name(payment.getRecurringExpense().getName())
                        .category(payment.getRecurringExpense().getCategory())
                        .expectedAmount(payment.getExpectedAmount())
                        .expectedDate(payment.getExpectedDate())
                        .matchScore(matchScore)
                        .daysDifference((int) daysDiff)
                        .amountDifferencePercent(amountDiffPercent)
                        .build());
            }
        }

        // 점수 높은 순 정렬
        candidates.sort(Comparator.comparingDouble(MatchCandidateResponse::getMatchScore).reversed());

        return candidates;
    }

    /**
     * 매칭 확정
     */
    @Transactional
    public RecurringExpensePaymentResponse confirmMatch(UUID paymentId, UUID expenseId, UUID userId) {
        RecurringExpensePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다"));

        // 권한 확인
        if (!payment.getRecurringExpense().getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 이미 확정된 경우
        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            throw new BusinessException("이미 확정된 결제입니다", ErrorCode.INVALID_INPUT);
        }

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("지출을 찾을 수 없습니다"));

        // 지출 권한 확인
        if (!expense.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // 이미 다른 결제에 연결된 경우
        if (expense.getLinkedPaymentId() != null) {
            throw new BusinessException("이미 다른 고정비에 연결된 지출입니다", ErrorCode.INVALID_INPUT);
        }

        // 매칭 확정
        payment.confirm(expense);
        expense.setLinkedPaymentId(payment.getPaymentId());

        // RecurringExpense 마스터 금액 갱신
        RecurringExpense re = payment.getRecurringExpense();
        re.setLastAmount(re.getAmount());
        re.setAmount(expense.getAmount());
        re.setLastPaymentDate(expense.getDate());

        paymentRepository.save(payment);
        expenseRepository.save(expense);
        recurringExpenseRepository.save(re);

        log.info("Matched expense {} with recurring payment {} (amount: {} -> {})",
                expenseId, paymentId, re.getLastAmount(), expense.getAmount());

        return toResponse(payment);
    }

    /**
     * 이번 달 건너뛰기
     */
    @Transactional
    public RecurringExpensePaymentResponse skipPayment(UUID paymentId, UUID userId) {
        RecurringExpensePayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다"));

        // 권한 확인
        if (!payment.getRecurringExpense().getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("PENDING 상태에서만 건너뛸 수 있습니다", ErrorCode.INVALID_INPUT);
        }

        payment.skip();
        paymentRepository.save(payment);

        log.info("Skipped payment {} for recurring expense {}",
                paymentId, payment.getRecurringExpense().getName());

        return toResponse(payment);
    }

    /**
     * 지출 삭제 시 연결된 결제 복원
     */
    @Transactional
    public void unlinkPaymentByExpense(UUID expenseId) {
        paymentRepository.findByExpense_ExpenseId(expenseId).ifPresent(payment -> {
            payment.unlink();
            paymentRepository.save(payment);
            log.info("Unlinked payment {} due to expense deletion", payment.getPaymentId());
        });
    }

    // === Private Methods ===

    private LocalDate calculateExpectedDate(RecurringExpense re, int year, int month) {
        Integer dayOfMonth = re.getDayOfMonth();
        if (dayOfMonth == null) {
            dayOfMonth = re.getNextPaymentDate().getDayOfMonth();
        }

        // 월의 마지막 날 처리 (31일이 없는 달 등)
        int lastDay = LocalDate.of(year, month, 1).lengthOfMonth();
        int day = Math.min(dayOfMonth, lastDay);

        return LocalDate.of(year, month, day);
    }

    private double calculateMatchScore(Expense expense, RecurringExpensePayment payment) {
        // 1. 날짜 차이 점수 (0~1)
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(
                expense.getDate(), payment.getExpectedDate()));

        if (daysDiff > DEFAULT_MATCH_WINDOW_DAYS) {
            return 0; // 윈도우 초과
        }

        double dateScore = 1.0 - (daysDiff / (double) DEFAULT_MATCH_WINDOW_DAYS);

        // 2. 금액 차이 점수 (0~1)
        double amountDiffPercent = calculateAmountDifferencePercent(
                expense.getAmount(), payment.getExpectedAmount());

        if (amountDiffPercent > DEFAULT_AMOUNT_TOLERANCE_PERCENT) {
            return 0; // 허용 범위 초과
        }

        double amountScore = 1.0 - (amountDiffPercent / DEFAULT_AMOUNT_TOLERANCE_PERCENT);

        // 3. 카테고리 일치 보너스
        double categoryBonus = 0;
        if (expense.getCategory() != null &&
                expense.getCategory().equals(payment.getRecurringExpense().getCategory())) {
            categoryBonus = 0.2;
        }

        // 가중 평균 (날짜 40%, 금액 40%, 카테고리 20%)
        return Math.min(1.0, dateScore * 0.4 + amountScore * 0.4 + categoryBonus);
    }

    private double calculateAmountDifferencePercent(BigDecimal actual, BigDecimal expected) {
        if (expected.compareTo(BigDecimal.ZERO) == 0) {
            return 100;
        }
        BigDecimal diff = actual.subtract(expected).abs();
        return diff.divide(expected, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private RecurringExpensePaymentResponse toResponse(RecurringExpensePayment payment) {
        return RecurringExpensePaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .recurringExpenseId(payment.getRecurringExpense().getRecurringExpenseId())
                .recurringExpenseName(payment.getRecurringExpense().getName())
                .category(payment.getRecurringExpense().getCategory())
                .expenseId(payment.getExpense() != null ? payment.getExpense().getExpenseId() : null)
                .periodYear(payment.getPeriodYear())
                .periodMonth(payment.getPeriodMonth())
                .expectedAmount(payment.getExpectedAmount())
                .actualAmount(payment.getActualAmount())
                .expectedDate(payment.getExpectedDate())
                .actualDate(payment.getActualDate())
                .status(payment.getStatus())
                .confirmedAt(payment.getConfirmedAt())
                .build();
    }
}
