package com.moneyflow.service;

import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.income.Income;
import com.moneyflow.domain.income.IncomeRepository;
import com.moneyflow.domain.accountbook.AccountBookMemberRepository;
import com.moneyflow.dto.response.DailySummaryDto;
import com.moneyflow.dto.response.TransactionDto;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final AccountBookMemberRepository accountBookMemberRepository;

    public Map<String, DailySummaryDto> getMonthlyData(
            UUID userId,
            UUID accountBookId,
            int year,
            int month) {
        // 1. 해당 월의 시작일과 종료일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        if (!accountBookMemberRepository
                .existsByAccountBookAccountBookIdAndUserUserId(accountBookId, userId)) {
            throw new UnauthorizedException("장부의 멤버만 조회할 수 있습니다");
        }

        // 2. 한 달 치 데이터 한 번에 조회 (DB 쿼리 2회)
        List<Expense> allExpenses = expenseRepository.findByAccountBookAndDateRange(
                accountBookId, startDate, endDate, null);
        List<Income> allIncomes = incomeRepository.findByAccountBookAndDateRange(
                accountBookId, startDate, endDate, null);

        // 3. 날짜별로 데이터 그룹화
        Map<LocalDate, List<Expense>> expensesByDate = allExpenses.stream()
                .collect(Collectors.groupingBy(Expense::getDate));

        Map<LocalDate, List<Income>> incomesByDate = allIncomes.stream()
                .collect(Collectors.groupingBy(Income::getDate));

        // 4. 모든 날짜 추출 (중복 제거)
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(expensesByDate.keySet());
        allDates.addAll(incomesByDate.keySet());

        // 5. 응답 DTO 생성 (결과 Map)
        Map<String, DailySummaryDto> resultMap = new HashMap<>();

        for (LocalDate date : allDates) {
            long totalIncome = 0;
            long totalExpense = 0;
            List<TransactionDto> transactionDtos = new ArrayList<>();

            // 해당 날짜의 지출 처리
            List<Expense> dayExpenses = expensesByDate.getOrDefault(date, Collections.emptyList());
            for (Expense expense : dayExpenses) {
                totalExpense += expense.getAmount().longValue();

                transactionDtos.add(TransactionDto.builder()
                        .id(expense.getExpenseId().toString())
                        .type("EXPENSE")
                        .amount(expense.getAmount().longValue())
                        .title(expense.getMerchant() != null ? expense.getMerchant() : expense.getCategory())
                        .category(expense.getCategory())
                        .memo(expense.getMemo())
                        .time("")
                        .build());
            }

            // 해당 날짜의 수입 처리
            List<Income> dayIncomes = incomesByDate.getOrDefault(date, Collections.emptyList());
            for (Income income : dayIncomes) {
                totalIncome += income.getAmount().longValue();

                transactionDtos.add(TransactionDto.builder()
                        .id(income.getIncomeId().toString())
                        .type("INCOME")
                        .amount(income.getAmount().longValue())
                        .title(income.getDescription() != null ? income.getDescription() : income.getSource())
                        .category(income.getSource())
                        .memo(null)
                        .time("")
                        .build());
            }

            // 해당 날짜의 Summary 생성
            DailySummaryDto summary = DailySummaryDto.builder()
                    .date(date.toString()) // "2025-12-24"
                    .totalIncome(totalIncome)
                    .totalExpense(totalExpense)
                    .transactions(transactionDtos)
                    .build();

            resultMap.put(date.toString(), summary);
        }

        return resultMap; // 프론트엔드가 원하는 Map 형태 반환
    }
}
