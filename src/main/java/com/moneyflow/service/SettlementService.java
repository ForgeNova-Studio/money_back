package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookMember;
import com.moneyflow.domain.accountbook.AccountBookMemberRepository;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseParticipant;
import com.moneyflow.domain.expense.ExpenseParticipantRepository;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.dto.response.SettlementResponse;
import com.moneyflow.dto.response.SettlementResponse.MemberSettlement;
import com.moneyflow.dto.response.SettlementResponse.SettlementTransaction;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 정산 서비스
 *
 * 여행 가계부 등의 공용 지출에 대한 N빵 정산 계산을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final AccountBookRepository accountBookRepository;
    private final AccountBookMemberRepository accountBookMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseParticipantRepository expenseParticipantRepository;

    /**
     * 정산 계산
     *
     * 로직:
     * 1. 공용 지출(SHARED_POOL)만 필터링
     * 2. 각 지출의 참여자별 분담금 계산
     * 3. 멤버별 "결제한 금액" vs "부담해야 할 금액" 비교
     * 4. 최소 거래로 정산하는 트랜잭션 생성
     *
     * @param userId        요청 사용자 ID
     * @param accountBookId 장부 ID
     * @return 정산 결과
     */
    @Transactional(readOnly = true)
    public SettlementResponse calculateSettlement(UUID userId, UUID accountBookId) {
        // 장부 조회 및 권한 확인
        AccountBook accountBook = accountBookRepository.findByIdWithMembersAndUsers(accountBookId)
                .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

        boolean isMember = accountBook.getMembers().stream()
                .anyMatch(m -> m.getUser().getUserId().equals(userId));
        if (!isMember) {
            throw UnauthorizedException.accessDenied("해당 장부에 접근할 권한이 없습니다");
        }

        // 장부의 모든 멤버 조회
        List<AccountBookMember> members = accountBook.getMembers();
        Map<UUID, String> memberNicknames = members.stream()
                .collect(Collectors.toMap(
                        m -> m.getUser().getUserId(),
                        m -> m.getUser().getNickname()));

        // 장부의 모든 지출 조회
        List<Expense> allExpenses = expenseRepository.findByAccountBookIdWithFetch(accountBookId);

        // 공용 지출과 개인 지출 분리
        List<Expense> sharedExpenses = allExpenses.stream()
                .filter(Expense::isSharedExpense)
                .toList();
        List<Expense> personalExpenses = allExpenses.stream()
                .filter(Expense::isPersonalExpense)
                .toList();

        BigDecimal totalSharedExpense = sharedExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPersonalExpense = personalExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 멤버별 결제 금액 및 분담 금액 계산
        Map<UUID, BigDecimal> paidAmounts = new HashMap<>(); // 각 멤버가 결제한 금액
        Map<UUID, BigDecimal> shouldPayAmounts = new HashMap<>(); // 각 멤버가 부담해야 할 금액

        // 초기화
        for (AccountBookMember member : members) {
            UUID memberId = member.getUser().getUserId();
            paidAmounts.put(memberId, BigDecimal.ZERO);
            shouldPayAmounts.put(memberId, BigDecimal.ZERO);
        }

        // 공용 지출별로 계산
        for (Expense expense : sharedExpenses) {
            // 결제자의 결제 금액 누적
            UUID payerId = expense.getPaidBy() != null
                    ? expense.getPaidBy().getUserId()
                    : expense.getUser().getUserId();
            paidAmounts.merge(payerId, expense.getAmount(), BigDecimal::add);

            // 참여자별 분담 금액 계산
            List<ExpenseParticipant> participants = expense.getParticipants();

            if (participants.isEmpty()) {
                // 참여자 미지정: 모든 멤버 균등 분담
                BigDecimal sharePerMember = expense.getAmount()
                        .divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
                for (AccountBookMember member : members) {
                    shouldPayAmounts.merge(member.getUser().getUserId(), sharePerMember, BigDecimal::add);
                }
            } else {
                // 참여자 지정: 참여자만 분담
                BigDecimal totalRatio = participants.stream()
                        .map(ExpenseParticipant::getShareRatio)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                for (ExpenseParticipant participant : participants) {
                    BigDecimal share = expense.getAmount()
                            .multiply(participant.getShareRatio())
                            .divide(totalRatio, 2, RoundingMode.HALF_UP);
                    shouldPayAmounts.merge(participant.getUser().getUserId(), share, BigDecimal::add);
                }
            }
        }

        // 멤버별 정산 내역 생성
        List<MemberSettlement> memberSettlements = new ArrayList<>();
        for (AccountBookMember member : members) {
            UUID memberId = member.getUser().getUserId();
            BigDecimal paid = paidAmounts.getOrDefault(memberId, BigDecimal.ZERO);
            BigDecimal shouldPay = shouldPayAmounts.getOrDefault(memberId, BigDecimal.ZERO);
            BigDecimal balance = paid.subtract(shouldPay);

            memberSettlements.add(MemberSettlement.builder()
                    .userId(memberId)
                    .nickname(memberNicknames.get(memberId))
                    .paidAmount(paid)
                    .shouldPayAmount(shouldPay)
                    .balance(balance)
                    .build());
        }

        // 정산 거래 계산 (최소 거래 알고리즘)
        List<SettlementTransaction> transactions = calculateTransactions(memberSettlements, memberNicknames);

        log.info("정산 계산 완료: accountBookId={}, sharedTotal={}, memberCount={}",
                accountBookId, totalSharedExpense, members.size());

        return SettlementResponse.builder()
                .accountBookId(accountBookId)
                .accountBookName(accountBook.getName())
                .totalSharedExpense(totalSharedExpense)
                .totalPersonalExpense(totalPersonalExpense)
                .members(memberSettlements)
                .transactions(transactions)
                .build();
    }

    /**
     * 최소 거래로 정산하는 트랜잭션 계산
     *
     * 알고리즘:
     * 1. 돈을 받아야 하는 사람(balance > 0)과 내야 하는 사람(balance < 0) 분리
     * 2. 가장 많이 받아야 하는 사람과 가장 많이 내야 하는 사람 매칭
     * 3. 둘 중 작은 금액만큼 거래 생성
     * 4. 반복
     */
    private List<SettlementTransaction> calculateTransactions(
            List<MemberSettlement> settlements,
            Map<UUID, String> nicknames) {

        List<SettlementTransaction> transactions = new ArrayList<>();

        // 복사본 생성 (원본 수정 방지)
        List<MemberSettlement> receivers = settlements.stream()
                .filter(s -> s.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .map(s -> MemberSettlement.builder()
                        .userId(s.getUserId())
                        .nickname(s.getNickname())
                        .balance(s.getBalance())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        List<MemberSettlement> payers = settlements.stream()
                .filter(s -> s.getBalance().compareTo(BigDecimal.ZERO) < 0)
                .map(s -> MemberSettlement.builder()
                        .userId(s.getUserId())
                        .nickname(s.getNickname())
                        .balance(s.getBalance().abs()) // 절대값으로 변환
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        // 금액 기준 내림차순 정렬
        receivers.sort((a, b) -> b.getBalance().compareTo(a.getBalance()));
        payers.sort((a, b) -> b.getBalance().compareTo(a.getBalance()));

        while (!receivers.isEmpty() && !payers.isEmpty()) {
            MemberSettlement receiver = receivers.get(0);
            MemberSettlement payer = payers.get(0);

            BigDecimal transferAmount = receiver.getBalance().min(payer.getBalance());

            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                transactions.add(SettlementTransaction.builder()
                        .fromUserId(payer.getUserId())
                        .fromNickname(payer.getNickname())
                        .toUserId(receiver.getUserId())
                        .toNickname(receiver.getNickname())
                        .amount(transferAmount)
                        .build());
            }

            // 잔액 업데이트
            receiver.setBalance(receiver.getBalance().subtract(transferAmount));
            payer.setBalance(payer.getBalance().subtract(transferAmount));

            // 완료된 사람 제거
            if (receiver.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                receivers.remove(0);
            }
            if (payer.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                payers.remove(0);
            }
        }

        return transactions;
    }
}
