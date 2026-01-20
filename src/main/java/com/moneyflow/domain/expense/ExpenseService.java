package com.moneyflow.domain.expense;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.accountbook.FundingSource;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.ExpenseRequest;
import com.moneyflow.dto.response.BulkExpenseResponse;
import com.moneyflow.dto.response.ExpenseListResponse;
import com.moneyflow.dto.response.ExpenseResponse;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import com.moneyflow.service.CategoryClassifier;
import com.moneyflow.service.RecurringExpenseMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository expenseParticipantRepository;
    private final UserRepository userRepository;
    private final AccountBookRepository accountBookRepository;
    private final CategoryClassifier categoryClassifier;
    private final RecurringExpenseMatchingService matchingService;

    /**
     * 지출 생성
     */
    @Transactional
    public ExpenseResponse createExpense(UUID userId, ExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        // 장부 ID가 제공된 경우 검증, 없으면 기본 장부 자동 할당
        AccountBook accountBook;
        if (request.getAccountBookId() != null) {
            accountBook = accountBookRepository.findById(request.getAccountBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

            // 사용자가 해당 장부의 멤버인지 확인
            boolean isMember = accountBook.getMembers().stream()
                    .anyMatch(member -> member.getUser().getUserId().equals(userId));

            if (!isMember) {
                throw new UnauthorizedException("해당 장부에 접근할 권한이 없습니다");
            }
        } else {
            // 기본 장부 자동 할당
            accountBook = accountBookRepository.findDefaultAccountBookByUserId(userId)
                    .orElse(null);
            if (accountBook != null) {
                log.info("Auto-assigned default account book {} for expense", accountBook.getAccountBookId());
            }
        }

        // 카테고리가 없으면 자동 분류
        String category = request.getCategory();
        boolean isAutoCategorized = false;

        if (category == null || category.trim().isEmpty()) {
            category = categoryClassifier.classify(request.getMerchant());
            isAutoCategorized = true;
            log.info("Auto-classified merchant '{}' as category '{}'", request.getMerchant(), category);
        }

        // paidBy 처리: 공용 지출이면 결제자 설정
        User paidBy = null;
        if (request.getFundingSource() == FundingSource.SHARED_POOL) {
            if (request.getPaidByUserId() != null) {
                paidBy = userRepository.findById(request.getPaidByUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("결제자를 찾을 수 없습니다"));
            } else {
                paidBy = user; // 결제자 미지정 시 등록자가 결제한 것으로 처리
            }
        }

        Expense expense = Expense.builder()
                .user(user)
                .paidBy(paidBy)
                .accountBook(accountBook)
                .fundingSource(request.getFundingSource())
                .amount(request.getAmount())
                .date(request.getDate())
                .category(category)
                .merchant(request.getMerchant())
                .memo(request.getMemo())
                .paymentMethod(request.getPaymentMethod())
                .imageUrl(request.getImageUrl())
                .isAutoCategorized(isAutoCategorized)
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        // 공용 지출이고 참여자가 지정된 경우 참여자 저장
        if (request.getFundingSource() == FundingSource.SHARED_POOL
                && request.getParticipants() != null
                && !request.getParticipants().isEmpty()) {
            List<ExpenseParticipant> participants = new ArrayList<>();
            for (ExpenseRequest.ParticipantInfo info : request.getParticipants()) {
                User participantUser = userRepository.findById(info.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("참여자를 찾을 수 없습니다: " + info.getUserId()));

                ExpenseParticipant participant = ExpenseParticipant.builder()
                        .id(new ExpenseParticipantId(savedExpense.getExpenseId(), info.getUserId()))
                        .expense(savedExpense)
                        .user(participantUser)
                        .shareRatio(info.getShareRatio() != null ? info.getShareRatio() : BigDecimal.ONE)
                        .build();
                participants.add(participant);
            }
            expenseParticipantRepository.saveAll(participants);
            savedExpense.setParticipants(participants);
            log.info("Created {} participants for expense {}", participants.size(), savedExpense.getExpenseId());
        }

        log.info("Created expense: {} linked to account book: {}", savedExpense.getExpenseId(),
                accountBook != null ? accountBook.getAccountBookId() : "none");

        return toResponse(savedExpense);
    }

    /**
     * 지출 목록 조회
     */
    @Transactional(readOnly = true)
    public ExpenseListResponse getExpenses(UUID userId, LocalDate startDate, LocalDate endDate, String category) {
        List<Expense> expenses = expenseRepository.findExpensesByUserAndDateRange(
                userId, startDate, endDate, category);

        List<ExpenseResponse> expenseResponses = expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        BigDecimal totalAmount = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ExpenseListResponse.builder()
                .expenses(expenseResponses)
                .totalAmount(totalAmount)
                .count(expenses.size())
                .build();
    }

    /**
     * 지출 상세 조회
     */
    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("지출 내역을 찾을 수 없습니다"));

        if (!expense.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("해당 지출 내역에 접근할 권한이 없습니다");
        }

        return toResponse(expense);
    }

    /**
     * 지출 수정
     */
    @Transactional
    public ExpenseResponse updateExpense(UUID userId, UUID expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("지출 내역을 찾을 수 없습니다"));

        if (!expense.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("해당 지출 내역을 수정할 권한이 없습니다");
        }

        // 업데이트
        expense.setAmount(request.getAmount());
        expense.setDate(request.getDate());
        expense.setCategory(request.getCategory());
        expense.setMerchant(request.getMerchant());
        expense.setMemo(request.getMemo());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setImageUrl(request.getImageUrl());

        if (request.getIsAutoCategorized() != null) {
            expense.setIsAutoCategorized(request.getIsAutoCategorized());
        }

        Expense updatedExpense = expenseRepository.save(expense);
        log.info("Updated expense: {}", expenseId);

        return toResponse(updatedExpense);
    }

    /**
     * 지출 삭제
     */
    @Transactional
    public void deleteExpense(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("지출 내역을 찾을 수 없습니다"));

        if (!expense.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("해당 지출 내역을 삭제할 권한이 없습니다");
        }

        // 연결된 고정비 결제가 있으면 PENDING으로 복원
        if (expense.getLinkedPaymentId() != null) {
            matchingService.unlinkPaymentByExpense(expenseId);
            log.info("Unlinked payment for deleted expense: {}", expenseId);
        }

        expenseRepository.delete(expense);
        log.info("Deleted expense: {}", expenseId);
    }

    /**
     * 최근 지출 내역 조회 (홈 화면용)
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getRecentExpenses(UUID userId) {
        List<Expense> expenses = expenseRepository.findTop5ByUserUserIdOrderByDateDescCreatedAtDesc(userId);

        return expenses.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 일괄 지출 생성 (OCR 결과 저장용)
     */
    @Transactional
    public BulkExpenseResponse createBulkExpenses(UUID userId, List<ExpenseRequest> requests) {
        List<ExpenseResponse> savedExpenses = new java.util.ArrayList<>();
        List<BulkExpenseResponse.FailedItem> failures = new java.util.ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            try {
                ExpenseResponse response = createExpense(userId, requests.get(i));
                savedExpenses.add(response);
            } catch (Exception e) {
                log.error("Failed to create expense at index {}: {}", i, e.getMessage());
                failures.add(BulkExpenseResponse.FailedItem.builder()
                        .index(i)
                        .reason(e.getMessage())
                        .build());
            }
        }

        log.info("Bulk expense creation: {} success, {} failed out of {} total",
                savedExpenses.size(), failures.size(), requests.size());

        return BulkExpenseResponse.builder()
                .totalRequested(requests.size())
                .successCount(savedExpenses.size())
                .failedCount(failures.size())
                .savedExpenses(savedExpenses)
                .failures(failures.isEmpty() ? null : failures)
                .build();
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.builder()
                .expenseId(expense.getExpenseId())
                .userId(expense.getUser().getUserId())
                .accountBookId(expense.getAccountBook() != null ? expense.getAccountBook().getAccountBookId() : null)
                .fundingSource(expense.getFundingSource() != null ? expense.getFundingSource().name() : null)
                .amount(expense.getAmount())
                .date(expense.getDate())
                .category(expense.getCategory())
                .merchant(expense.getMerchant())
                .memo(expense.getMemo())
                .paymentMethod(expense.getPaymentMethod())
                .imageUrl(expense.getImageUrl())
                .isAutoCategorized(expense.getIsAutoCategorized())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}
