package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.accountbook.FundingSource;
import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.dto.response.ExpenseResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 지출 이동 서비스
 *
 * 여행 가계부 등에서 개인 지출을 본인의 기본 가계부로 이동하는 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseTransferService {

    private final ExpenseRepository expenseRepository;
    private final AccountBookRepository accountBookRepository;

    /**
     * 단건 지출 이동
     *
     * @param userId              요청 사용자 ID
     * @param expenseId           이동할 지출 ID
     * @param targetAccountBookId 대상 장부 ID
     * @return 이동된 지출 정보
     */
    @Transactional
    public ExpenseResponse transferExpense(UUID userId, UUID expenseId, UUID targetAccountBookId) {
        // 지출 조회
        Expense expense = expenseRepository.findByIdWithUserAndAccountBook(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("지출을 찾을 수 없습니다"));

        // 본인 지출인지 확인
        if (!expense.getUser().getUserId().equals(userId)) {
            throw UnauthorizedException.accessDenied("본인의 지출만 이동할 수 있습니다");
        }

        // 개인 지출인지 확인
        if (!expense.isPersonalExpense()) {
            throw new BusinessException("개인 지출만 이동할 수 있습니다. 공용 지출은 정산 후 이동하세요.", ErrorCode.INVALID_INPUT);
        }

        // 이미 이동된 지출인지 확인
        if (expense.isTransferred()) {
            throw new BusinessException("이미 이동된 지출입니다", ErrorCode.INVALID_INPUT);
        }

        // 대상 장부 조회 및 권한 확인
        AccountBook targetBook = accountBookRepository.findByIdWithMembersAndUsers(targetAccountBookId)
                .orElseThrow(() -> new ResourceNotFoundException("대상 장부를 찾을 수 없습니다"));

        boolean isMember = targetBook.getMembers().stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
        if (!isMember) {
            throw UnauthorizedException.accessDenied("대상 장부에 접근할 권한이 없습니다");
        }

        // 원본 장부 ID 저장 후 이동
        UUID originalBookId = expense.getAccountBook() != null
                ? expense.getAccountBook().getAccountBookId()
                : null;

        expense.setOriginalAccountBookId(originalBookId);
        expense.setAccountBook(targetBook);

        Expense savedExpense = expenseRepository.save(expense);

        log.info("지출 이동 완료: expenseId={}, from={}, to={}",
                expenseId, originalBookId, targetAccountBookId);

        return toResponse(savedExpense);
    }

    /**
     * 여행 장부의 모든 개인 지출 일괄 이동
     *
     * @param userId              요청 사용자 ID
     * @param sourceAccountBookId 원본 장부 ID (여행 장부)
     * @param targetAccountBookId 대상 장부 ID (개인 장부)
     * @return 이동 결과
     */
    @Transactional
    public BulkTransferResult transferAllPersonalExpenses(
            UUID userId,
            UUID sourceAccountBookId,
            UUID targetAccountBookId) {

        // 원본 장부 권한 확인
        AccountBook sourceBook = accountBookRepository.findByIdWithMembersAndUsers(sourceAccountBookId)
                .orElseThrow(() -> new ResourceNotFoundException("원본 장부를 찾을 수 없습니다"));

        boolean isSourceMember = sourceBook.getMembers().stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
        if (!isSourceMember) {
            throw UnauthorizedException.accessDenied("원본 장부에 접근할 권한이 없습니다");
        }

        // 대상 장부 권한 확인
        AccountBook targetBook = accountBookRepository.findByIdWithMembersAndUsers(targetAccountBookId)
                .orElseThrow(() -> new ResourceNotFoundException("대상 장부를 찾을 수 없습니다"));

        boolean isTargetMember = targetBook.getMembers().stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
        if (!isTargetMember) {
            throw UnauthorizedException.accessDenied("대상 장부에 접근할 권한이 없습니다");
        }

        // 원본 장부의 본인 개인 지출 조회
        List<Expense> allExpenses = expenseRepository.findByAccountBookIdWithFetch(sourceAccountBookId);
        List<Expense> myPersonalExpenses = allExpenses.stream()
                .filter(e -> e.getUser().getUserId().equals(userId))
                .filter(Expense::isPersonalExpense)
                .filter(e -> !e.isTransferred())
                .toList();

        // 일괄 이동
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        for (Expense expense : myPersonalExpenses) {
            try {
                expense.setOriginalAccountBookId(sourceAccountBookId);
                expense.setAccountBook(targetBook);
                expenseRepository.save(expense);
                successCount++;
            } catch (Exception e) {
                errors.add(expense.getExpenseId() + ": " + e.getMessage());
            }
        }

        log.info("일괄 이동 완료: sourceBookId={}, targetBookId={}, total={}, success={}",
                sourceAccountBookId, targetAccountBookId, myPersonalExpenses.size(), successCount);

        return BulkTransferResult.builder()
                .totalCount(myPersonalExpenses.size())
                .successCount(successCount)
                .failedCount(errors.size())
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }

    /**
     * Entity → Response DTO 변환
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

    /**
     * 일괄 이동 결과
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkTransferResult {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private List<String> errors;
    }
}
