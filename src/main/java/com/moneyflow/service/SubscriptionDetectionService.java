package com.moneyflow.service;

import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.recurringexpense.RecurringExpense;
import com.moneyflow.domain.recurringexpense.RecurringExpenseRepository;
import com.moneyflow.domain.recurringexpense.RecurringType;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 구독료 자동 탐지 서비스
 * 사용자의 지출 내역을 분석하여 반복적인 패턴을 찾아 구독료로 등록
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionDetectionService {

    private final ExpenseRepository expenseRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final UserRepository userRepository;
    private final com.moneyflow.domain.accountbook.AccountBookRepository accountBookRepository;

    // 유명 구독 서비스 키워드 맵
    private static final Map<String, String> SUBSCRIPTION_KEYWORDS = new HashMap<>() {
        {
            // 스트리밍
            put("넷플릭스", "문화/여가");
            put("netflix", "문화/여가");
            put("유튜브", "문화/여가");
            put("youtube", "문화/여가");
            put("youtube premium", "문화/여가");
            put("디즈니", "문화/여가");
            put("disney", "문화/여가");
            put("왓챠", "문화/여가");
            put("watcha", "문화/여가");
            put("웨이브", "문화/여가");
            put("wavve", "문화/여가");
            put("티빙", "문화/여가");
            put("tving", "문화/여가");

            // 음악
            put("스포티파이", "문화/여가");
            put("spotify", "문화/여가");
            put("멜론", "문화/여가");
            put("melon", "문화/여가");
            put("지니", "문화/여가");
            put("genie", "문화/여가");
            put("애플뮤직", "문화/여가");
            put("apple music", "문화/여가");

            // 클라우드/소프트웨어
            put("아이클라우드", "기타");
            put("icloud", "기타");
            put("구글드라이브", "기타");
            put("google drive", "기타");
            put("google one", "기타");
            put("드롭박스", "기타");
            put("dropbox", "기타");
            put("notion", "기타");
            put("노션", "기타");

            // 배달/멤버십
            put("쿠팡", "기타");
            put("coupang", "기타");
            put("쿠팡이츠", "식비");
            put("배달의민족", "식비");
            put("배민", "식비");
            put("요기요", "식비");
        }
    };

    /**
     * 사용자의 지출 내역을 분석하여 구독료 자동 탐지
     * 
     * @param userId          사용자 ID
     * @param monthsToAnalyze 분석할 과거 개월 수 (기본: 3개월)
     * @return 탐지된 구독료 목록
     */
    @Transactional
    public List<RecurringExpense> detectSubscriptions(UUID userId, Integer monthsToAnalyze) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int months = monthsToAnalyze != null ? monthsToAnalyze : 3;
        LocalDate startDate = LocalDate.now().minusMonths(months);

        // 분석 기간 내 모든 지출 조회
        List<Expense> expenses = expenseRepository.findByUser_UserIdAndDateAfter(userId, startDate);

        log.info("구독료 탐지 시작: 사용자 {}, 지출 건수: {}, 분석 기간: {} 개월", userId, expenses.size(), months);

        // 가맹점별 지출 그룹화
        Map<String, List<Expense>> expensesByMerchant = expenses.stream()
                .filter(e -> e.getMerchant() != null && !e.getMerchant().isEmpty())
                .collect(Collectors.groupingBy(Expense::getMerchant));

        List<RecurringExpense> detectedSubscriptions = new ArrayList<>();

        // 각 가맹점별 패턴 분석
        for (Map.Entry<String, List<Expense>> entry : expensesByMerchant.entrySet()) {
            String merchant = entry.getKey();
            List<Expense> merchantExpenses = entry.getValue();

            // 최소 2회 이상 결제가 있어야 패턴으로 인식
            if (merchantExpenses.size() < 2) {
                continue;
            }

            // 구독료 패턴 감지
            Optional<SubscriptionPattern> pattern = analyzePattern(merchantExpenses);

            if (pattern.isPresent() && pattern.get().isSubscription()) {
                SubscriptionPattern p = pattern.get();

                // 이미 등록된 구독인지 확인
                Optional<RecurringExpense> existing = recurringExpenseRepository
                        .findByUser_UserIdAndSubscriptionProvider(userId, merchant);

                if (existing.isEmpty()) {
                    // 새로운 구독료 등록
                    RecurringExpense subscription = createSubscriptionFromPattern(user, merchant, p);
                    RecurringExpense saved = recurringExpenseRepository.save(subscription);
                    detectedSubscriptions.add(saved);

                    log.info("구독료 탐지 완료: {} (신뢰도: {})", merchant, p.getConfidence());
                }
            }
        }

        log.info("구독료 탐지 완료: {} 건 탐지", detectedSubscriptions.size());
        return detectedSubscriptions;
    }

    /**
     * 지출 패턴 분석
     */
    private Optional<SubscriptionPattern> analyzePattern(List<Expense> expenses) {
        if (expenses.size() < 2) {
            return Optional.empty();
        }

        // 날짜순 정렬
        expenses.sort(Comparator.comparing(Expense::getDate));

        // 금액 일관성 확인 (평균 대비 편차 10% 이내)
        BigDecimal avgAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        boolean isAmountConsistent = expenses.stream()
                .allMatch(e -> {
                    BigDecimal diff = e.getAmount().subtract(avgAmount).abs();
                    BigDecimal threshold = avgAmount.multiply(BigDecimal.valueOf(0.1));
                    return diff.compareTo(threshold) <= 0;
                });

        if (!isAmountConsistent) {
            return Optional.empty();
        }

        // 결제 주기 분석 (날짜 간격)
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < expenses.size(); i++) {
            long daysBetween = ChronoUnit.DAYS.between(
                    expenses.get(i - 1).getDate(),
                    expenses.get(i).getDate());
            intervals.add(daysBetween);
        }

        // 평균 간격 계산
        double avgInterval = intervals.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        // 주기 타입 판단
        RecurringType recurringType;
        if (avgInterval >= 350 && avgInterval <= 380) {
            recurringType = RecurringType.YEARLY;
        } else if (avgInterval >= 25 && avgInterval <= 35) {
            recurringType = RecurringType.MONTHLY;
        } else if (avgInterval >= 5 && avgInterval <= 9) {
            recurringType = RecurringType.WEEKLY;
        } else {
            return Optional.empty(); // 주기가 불규칙함
        }

        // 신뢰도 계산
        BigDecimal confidence = calculateConfidence(expenses, avgAmount, avgInterval);

        // 최소 신뢰도 0.7 이상이어야 구독으로 인정
        if (confidence.compareTo(BigDecimal.valueOf(0.7)) < 0) {
            return Optional.empty();
        }

        // 다음 결제 예상일 계산
        LocalDate lastPaymentDate = expenses.get(expenses.size() - 1).getDate();
        LocalDate nextPaymentDate = calculateNextPaymentDate(lastPaymentDate, recurringType);

        // 카테고리 추론
        String category = inferCategory(expenses.get(0).getMerchant());

        return Optional.of(SubscriptionPattern.builder()
                .amount(avgAmount)
                .recurringType(recurringType)
                .lastPaymentDate(lastPaymentDate)
                .nextPaymentDate(nextPaymentDate)
                .category(category)
                .confidence(confidence)
                .build());
    }

    /**
     * 신뢰도 계산
     */
    private BigDecimal calculateConfidence(List<Expense> expenses, BigDecimal avgAmount, double avgInterval) {
        // 기본 신뢰도: 0.5
        double confidence = 0.5;

        // 결제 횟수가 많을수록 신뢰도 증가 (최대 +0.3)
        confidence += Math.min(expenses.size() * 0.05, 0.3);

        // 금액 일관성 (편차가 작을수록 신뢰도 증가, 최대 +0.1)
        double amountVariance = expenses.stream()
                .mapToDouble(e -> e.getAmount().subtract(avgAmount).abs().doubleValue())
                .average()
                .orElse(0);
        double amountConsistency = 1 - (amountVariance / avgAmount.doubleValue());
        confidence += Math.min(amountConsistency * 0.1, 0.1);

        // 알려진 구독 서비스인 경우 신뢰도 증가 (+0.1)
        String merchant = expenses.get(0).getMerchant().toLowerCase();
        boolean isKnownService = SUBSCRIPTION_KEYWORDS.keySet().stream()
                .anyMatch(merchant::contains);
        if (isKnownService) {
            confidence += 0.1;
        }

        return BigDecimal.valueOf(Math.min(confidence, 1.0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 다음 결제일 계산
     */
    private LocalDate calculateNextPaymentDate(LocalDate lastPaymentDate, RecurringType recurringType) {
        return switch (recurringType) {
            case MONTHLY -> lastPaymentDate.plusMonths(1);
            case YEARLY -> lastPaymentDate.plusYears(1);
            case WEEKLY -> lastPaymentDate.plusWeeks(1);
        };
    }

    /**
     * 카테고리 추론
     */
    private String inferCategory(String merchant) {
        String lowerDesc = merchant.toLowerCase();

        for (Map.Entry<String, String> entry : SUBSCRIPTION_KEYWORDS.entrySet()) {
            if (lowerDesc.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "기타";
    }

    /**
     * 패턴으로부터 RecurringExpense 생성
     */
    private RecurringExpense createSubscriptionFromPattern(User user, String merchant, SubscriptionPattern pattern) {
        // 사용자의 기본 장부 조회
        com.moneyflow.domain.accountbook.AccountBook defaultAccountBook = accountBookRepository
                .findDefaultAccountBookByUserId(user.getUserId())
                .orElse(null);

        return RecurringExpense.builder()
                .user(user)
                .accountBook(defaultAccountBook)
                .name(merchant)
                .amount(pattern.getAmount())
                .category(pattern.getCategory())
                .description("자동 탐지된 구독료")
                .recurringType(pattern.getRecurringType())
                .startDate(pattern.getLastPaymentDate())
                .nextPaymentDate(pattern.getNextPaymentDate())
                .isSubscription(true)
                .subscriptionProvider(merchant)
                .notificationEnabled(true)
                .autoDetected(true)
                .detectionConfidence(pattern.getConfidence())
                .lastPaymentDate(pattern.getLastPaymentDate())
                .build();
    }

    /**
     * 구독료 패턴 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    private static class SubscriptionPattern {
        private BigDecimal amount;
        private RecurringType recurringType;
        private LocalDate lastPaymentDate;
        private LocalDate nextPaymentDate;
        private String category;
        private BigDecimal confidence;

        public boolean isSubscription() {
            return confidence.compareTo(BigDecimal.valueOf(0.7)) >= 0;
        }
    }
}
