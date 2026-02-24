package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.recurringexpense.RecurringExpense;
import com.moneyflow.domain.recurringexpense.RecurringExpenseRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.CreateRecurringExpenseRequest;
import com.moneyflow.dto.request.UpdateRecurringExpenseRequest;
import com.moneyflow.dto.response.MonthlyRecurringTotalResponse;
import com.moneyflow.dto.response.RecurringExpenseResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 고정비 및 구독료 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final UserRepository userRepository;
    private final AccountBookRepository accountBookRepository;

    /**
     * 고정비 등록
     * accountBookId 필수 (새로 생성하는 고정비는 장부에 연결되어야 함)
     */
    @Transactional
    public RecurringExpenseResponse createRecurringExpense(UUID userId, CreateRecurringExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // 장부 검증 (필수)
        if (request.getAccountBookId() == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_BOOK_REQUIRED);
        }

        AccountBook accountBook = accountBookRepository.findById(request.getAccountBookId())
                .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

        // 사용자가 해당 장부의 멤버인지 확인
        boolean isMember = accountBook.getMembers().stream()
                .anyMatch(member -> member.getUser().getUserId().equals(userId));

        if (!isMember) {
            throw UnauthorizedException.accessDenied("해당 장부에 접근할 권한이 없습니다");
        }

        RecurringExpense recurringExpense = RecurringExpense.builder()
                .user(user)
                .accountBook(accountBook)
                .name(request.getName())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .recurringType(request.getRecurringType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .dayOfMonth(request.getDayOfMonth())
                .dayOfWeek(request.getDayOfWeek())
                .nextPaymentDate(request.getNextPaymentDate())
                .isSubscription(request.getIsSubscription() != null ? request.getIsSubscription() : false)
                .subscriptionProvider(request.getSubscriptionProvider())
                .notificationEnabled(request.getNotificationEnabled() != null ? request.getNotificationEnabled() : true)
                .autoDetected(false)
                .build();

        RecurringExpense saved = recurringExpenseRepository.save(recurringExpense);
        log.info("고정비 등록 완료: {}, 사용자: {}, 장부: {}", saved.getName(), userId, accountBook.getAccountBookId());

        return toResponse(saved);
    }

    /**
     * 고정비 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getRecurringExpenses(UUID userId, Boolean includeShared) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        List<RecurringExpense> expenses;

        if (Boolean.TRUE.equals(includeShared)) {
            // 공유 모드: 내 고정비 + 내가 멤버인 장부의 고정비
            expenses = recurringExpenseRepository.findByUserOrSharedAccountBooks(userId);
        } else {
            // 개인 모드: 내 고정비만
            expenses = recurringExpenseRepository.findByUser_UserId(userId);
        }

        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 활성 고정비 목록 조회 (종료되지 않은 항목만)
     */
    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getActiveRecurringExpenses(UUID userId, Boolean includeShared) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        List<RecurringExpense> expenses;

        if (Boolean.TRUE.equals(includeShared)) {
            expenses = recurringExpenseRepository.findActiveRecurringExpensesWithSharedBooks(userId);
        } else {
            expenses = recurringExpenseRepository.findActiveRecurringExpenses(userId);
        }

        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 구독료만 조회
     */
    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getSubscriptions(UUID userId) {
        List<RecurringExpense> subscriptions = recurringExpenseRepository
                .findByUser_UserIdAndIsSubscription(userId, true);

        return subscriptions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 다가오는 결제 조회 (N일 이내)
     */
    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getUpcomingPayments(UUID userId, Integer daysAhead, Boolean includeShared) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead != null ? daysAhead : 7);

        List<RecurringExpense> expenses;

        if (Boolean.TRUE.equals(includeShared)) {
            expenses = recurringExpenseRepository.findUpcomingPaymentsWithSharedBooks(userId, today, endDate);
        } else {
            expenses = recurringExpenseRepository.findUpcomingPayments(userId, today, endDate);
        }

        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 고정비 상세 조회
     */
    @Transactional(readOnly = true)
    public RecurringExpenseResponse getRecurringExpense(UUID userId, UUID recurringExpenseId) {
        RecurringExpense expense = recurringExpenseRepository.findById(recurringExpenseId)
                .orElseThrow(() -> new ResourceNotFoundException("고정비를 찾을 수 없습니다: " + recurringExpenseId));

        // 권한 확인: 본인의 고정비이거나, 해당 장부의 멤버여야 함
        boolean isOwner = expense.getUser().getUserId().equals(userId);
        boolean isAccountBookMember = expense.getAccountBook() != null &&
                expense.getAccountBook().getMembers().stream()
                        .anyMatch(m -> m.getUser().getUserId().equals(userId));

        if (!isOwner && !isAccountBookMember) {
            throw UnauthorizedException.accessDenied("해당 고정비에 접근할 권한이 없습니다");
        }

        return toResponse(expense);
    }

    /**
     * 고정비 수정
     */
    @Transactional
    public RecurringExpenseResponse updateRecurringExpense(
            UUID userId, UUID recurringExpenseId, UpdateRecurringExpenseRequest request) {

        RecurringExpense expense = recurringExpenseRepository.findById(recurringExpenseId)
                .orElseThrow(() -> new ResourceNotFoundException("고정비를 찾을 수 없습니다: " + recurringExpenseId));

        // 권한 확인: 본인의 고정비만 수정 가능
        if (!expense.getUser().getUserId().equals(userId)) {
            throw UnauthorizedException.accessDenied("해당 고정비를 수정할 권한이 없습니다");
        }

        // 금액이 변경되면 이전 금액 저장
        if (request.getAmount() != null && !request.getAmount().equals(expense.getAmount())) {
            expense.setLastAmount(expense.getAmount());
            expense.setAmount(request.getAmount());
        }

        // 필드 업데이트 (null이 아닌 값만)
        if (request.getName() != null)
            expense.setName(request.getName());
        if (request.getCategory() != null)
            expense.setCategory(request.getCategory());
        if (request.getDescription() != null)
            expense.setDescription(request.getDescription());
        if (request.getRecurringType() != null)
            expense.setRecurringType(request.getRecurringType());
        if (request.getStartDate() != null)
            expense.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            expense.setEndDate(request.getEndDate());
        if (request.getDayOfMonth() != null)
            expense.setDayOfMonth(request.getDayOfMonth());
        if (request.getDayOfWeek() != null)
            expense.setDayOfWeek(request.getDayOfWeek());
        if (request.getNextPaymentDate() != null)
            expense.setNextPaymentDate(request.getNextPaymentDate());
        if (request.getIsSubscription() != null)
            expense.setIsSubscription(request.getIsSubscription());
        if (request.getSubscriptionProvider() != null)
            expense.setSubscriptionProvider(request.getSubscriptionProvider());
        if (request.getNotificationEnabled() != null)
            expense.setNotificationEnabled(request.getNotificationEnabled());

        RecurringExpense updated = recurringExpenseRepository.save(expense);
        log.info("고정비 수정 완료: {}, 사용자: {}", updated.getName(), userId);

        return toResponse(updated);
    }

    /**
     * 고정비 삭제
     */
    @Transactional
    public void deleteRecurringExpense(UUID userId, UUID recurringExpenseId) {
        RecurringExpense expense = recurringExpenseRepository.findById(recurringExpenseId)
                .orElseThrow(() -> new ResourceNotFoundException("고정비를 찾을 수 없습니다: " + recurringExpenseId));

        // 권한 확인: 본인의 고정비만 삭제 가능
        if (!expense.getUser().getUserId().equals(userId)) {
            throw UnauthorizedException.accessDenied("해당 고정비를 삭제할 권한이 없습니다");
        }

        recurringExpenseRepository.delete(expense);
        log.info("고정비 삭제 완료: {}, 사용자: {}", expense.getName(), userId);
    }

    /**
     * 월간 고정비 총액 조회
     */
    @Transactional(readOnly = true)
    public MonthlyRecurringTotalResponse getMonthlyTotal(UUID userId, Boolean includeShared) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        BigDecimal total;
        int count;

        if (Boolean.TRUE.equals(includeShared)) {
            // 공유 모드
            total = recurringExpenseRepository.calculateMonthlyTotalWithSharedBooks(userId);
            List<RecurringExpense> expenses = recurringExpenseRepository
                    .findActiveRecurringExpensesWithSharedBooks(userId);
            count = (int) expenses.stream()
                    .filter(e -> "MONTHLY".equals(e.getRecurringType().name()))
                    .count();
        } else {
            // 개인 모드
            total = recurringExpenseRepository.calculateMonthlyTotal(userId);
            List<RecurringExpense> expenses = recurringExpenseRepository.findActiveRecurringExpenses(userId);
            count = (int) expenses.stream()
                    .filter(e -> "MONTHLY".equals(e.getRecurringType().name()))
                    .count();
        }

        return MonthlyRecurringTotalResponse.builder()
                .monthlyTotal(total)
                .count(count)
                .isCoupleMode(Boolean.TRUE.equals(includeShared))
                .build();
    }

    /**
     * 다음 결제 날짜 자동 계산
     */
    @Transactional
    public void updateNextPaymentDates() {
        List<RecurringExpense> allExpenses = recurringExpenseRepository.findAll();

        for (RecurringExpense expense : allExpenses) {
            if (!expense.isExpired()) {
                expense.calculateNextPaymentDate();
                recurringExpenseRepository.save(expense);
            }
        }

        log.info("다음 결제 날짜 업데이트 완료: {} 건", allExpenses.size());
    }

    /**
     * Entity → Response DTO 변환
     */
    private RecurringExpenseResponse toResponse(RecurringExpense expense) {
        return RecurringExpenseResponse.builder()
                .recurringExpenseId(expense.getRecurringExpenseId())
                .userId(expense.getUser().getUserId())
                .accountBookId(expense.getAccountBook() != null ? expense.getAccountBook().getAccountBookId() : null)
                .name(expense.getName())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .recurringType(expense.getRecurringType())
                .startDate(expense.getStartDate())
                .endDate(expense.getEndDate())
                .dayOfMonth(expense.getDayOfMonth())
                .dayOfWeek(expense.getDayOfWeek())
                .nextPaymentDate(expense.getNextPaymentDate())
                .isSubscription(expense.getIsSubscription())
                .subscriptionProvider(expense.getSubscriptionProvider())
                .notificationEnabled(expense.getNotificationEnabled())
                .lastAmount(expense.getLastAmount())
                .lastPaymentDate(expense.getLastPaymentDate())
                .autoDetected(expense.getAutoDetected())
                .detectionConfidence(expense.getDetectionConfidence())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .hasAmountChanged(expense.hasAmountChanged())
                .isExpired(expense.isExpired())
                .build();
    }
}
