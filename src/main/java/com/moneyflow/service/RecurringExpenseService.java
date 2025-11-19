package com.moneyflow.service;

import com.moneyflow.domain.couple.Couple;
import com.moneyflow.domain.couple.CoupleRepository;
import com.moneyflow.domain.recurringexpense.RecurringExpense;
import com.moneyflow.domain.recurringexpense.RecurringExpenseRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.CreateRecurringExpenseRequest;
import com.moneyflow.dto.request.UpdateRecurringExpenseRequest;
import com.moneyflow.dto.response.MonthlyRecurringTotalResponse;
import com.moneyflow.dto.response.RecurringExpenseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    private final CoupleRepository coupleRepository;

    /**
     * 사용자의 커플 ID 조회 (있으면 반환, 없으면 null)
     */
    private UUID getUserCoupleId(UUID userId) {
        return coupleRepository.findByUserId(userId)
                .map(Couple::getCoupleId)
                .orElse(null);
    }

    /**
     * 고정비 등록
     */
    @Transactional
    public RecurringExpenseResponse createRecurringExpense(UUID userId, CreateRecurringExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        RecurringExpense recurringExpense = RecurringExpense.builder()
                .user(user)
                .coupleId(getUserCoupleId(userId))
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
        log.info("고정비 등록 완료: {}, 사용자: {}", saved.getName(), userId);

        return toResponse(saved);
    }

    /**
     * 고정비 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getRecurringExpenses(UUID userId, Boolean includeCouple) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        List<RecurringExpense> expenses;

        UUID coupleId = getUserCoupleId(userId);
        if (Boolean.TRUE.equals(includeCouple) && coupleId != null) {
            // 커플 모드: 내 고정비 + 커플 고정비
            expenses = recurringExpenseRepository.findByUserIdOrCoupleId(userId, coupleId);
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
    public List<RecurringExpenseResponse> getActiveRecurringExpenses(UUID userId, Boolean includeCouple) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        List<RecurringExpense> expenses;

        UUID coupleId = getUserCoupleId(userId);
        if (Boolean.TRUE.equals(includeCouple) && coupleId != null) {
            expenses = recurringExpenseRepository.findActiveRecurringExpensesWithCouple(userId, coupleId);
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
    public List<RecurringExpenseResponse> getUpcomingPayments(UUID userId, Integer daysAhead, Boolean includeCouple) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead != null ? daysAhead : 7);

        List<RecurringExpense> expenses;

        UUID coupleId = getUserCoupleId(userId);
        if (Boolean.TRUE.equals(includeCouple) && coupleId != null) {
            expenses = recurringExpenseRepository.findUpcomingPaymentsWithCouple(
                    userId, coupleId, today, endDate);
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
                .orElseThrow(() -> new IllegalArgumentException("고정비를 찾을 수 없습니다: " + recurringExpenseId));

        // 권한 확인: 본인의 고정비이거나, 커플 고정비여야 함
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        UUID coupleId = getUserCoupleId(userId);
        boolean isOwner = expense.getUser().getUserId().equals(userId);
        boolean isCoupleExpense = expense.getCoupleId() != null &&
                expense.getCoupleId().equals(coupleId);

        if (!isOwner && !isCoupleExpense) {
            throw new IllegalArgumentException("해당 고정비에 접근할 권한이 없습니다");
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
                .orElseThrow(() -> new IllegalArgumentException("고정비를 찾을 수 없습니다: " + recurringExpenseId));

        // 권한 확인: 본인의 고정비만 수정 가능
        if (!expense.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 고정비를 수정할 권한이 없습니다");
        }

        // 금액이 변경되면 이전 금액 저장
        if (request.getAmount() != null && !request.getAmount().equals(expense.getAmount())) {
            expense.setLastAmount(expense.getAmount());
            expense.setAmount(request.getAmount());
        }

        // 필드 업데이트 (null이 아닌 값만)
        if (request.getName() != null) expense.setName(request.getName());
        if (request.getCategory() != null) expense.setCategory(request.getCategory());
        if (request.getDescription() != null) expense.setDescription(request.getDescription());
        if (request.getRecurringType() != null) expense.setRecurringType(request.getRecurringType());
        if (request.getStartDate() != null) expense.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) expense.setEndDate(request.getEndDate());
        if (request.getDayOfMonth() != null) expense.setDayOfMonth(request.getDayOfMonth());
        if (request.getDayOfWeek() != null) expense.setDayOfWeek(request.getDayOfWeek());
        if (request.getNextPaymentDate() != null) expense.setNextPaymentDate(request.getNextPaymentDate());
        if (request.getIsSubscription() != null) expense.setIsSubscription(request.getIsSubscription());
        if (request.getSubscriptionProvider() != null) expense.setSubscriptionProvider(request.getSubscriptionProvider());
        if (request.getNotificationEnabled() != null) expense.setNotificationEnabled(request.getNotificationEnabled());

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
                .orElseThrow(() -> new IllegalArgumentException("고정비를 찾을 수 없습니다: " + recurringExpenseId));

        // 권한 확인: 본인의 고정비만 삭제 가능
        if (!expense.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 고정비를 삭제할 권한이 없습니다");
        }

        recurringExpenseRepository.delete(expense);
        log.info("고정비 삭제 완료: {}, 사용자: {}", expense.getName(), userId);
    }

    /**
     * 월간 고정비 총액 조회
     */
    @Transactional(readOnly = true)
    public MonthlyRecurringTotalResponse getMonthlyTotal(UUID userId, Boolean includeCouple) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        BigDecimal total;
        int count;

        UUID coupleId = getUserCoupleId(userId);
        if (Boolean.TRUE.equals(includeCouple) && coupleId != null) {
            // 커플 모드
            total = recurringExpenseRepository.calculateMonthlyTotalWithCouple(userId, coupleId);
            List<RecurringExpense> expenses = recurringExpenseRepository
                    .findActiveRecurringExpensesWithCouple(userId, coupleId);
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
                .isCoupleMode(Boolean.TRUE.equals(includeCouple) && coupleId != null)
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
                .coupleId(expense.getCoupleId())
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
