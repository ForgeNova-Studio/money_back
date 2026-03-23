package com.moneyflow.service;

import com.moneyflow.domain.expense.Expense;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.income.Income;
import com.moneyflow.domain.income.IncomeRepository;
import com.moneyflow.domain.accountbook.AccountBookMemberRepository;
import com.moneyflow.dto.response.DailySummaryDto;
import com.moneyflow.dto.response.SearchResponse;
import com.moneyflow.dto.response.TransactionDto;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final AccountBookMemberRepository accountBookMemberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
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
            throw new BusinessException(ErrorCode.ACCOUNT_BOOK_ACCESS_DENIED);
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

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public SearchResponse searchTransactions(
            UUID userId,
            UUID accountBookId,
            String keyword,
            int page,
            int size) {
        if (!accountBookMemberRepository
                .existsByAccountBookAccountBookIdAndUserUserId(accountBookId, userId)) {
            throw new BusinessException(ErrorCode.ACCOUNT_BOOK_ACCESS_DENIED);
        }

        String likeKeyword = "%" + escapeLikeKeyword(keyword.toLowerCase()) + "%";
        String bookId = accountBookId.toString();

        // totalCount: DB에서 UNION ALL 전체 건수 조회
        String countSql = """
                SELECT COUNT(*) FROM (
                  SELECT expense_id FROM expenses
                  WHERE account_book_id = :bookId::uuid
                    AND (LOWER(COALESCE(merchant, '')) LIKE :kw ESCAPE '\\' OR LOWER(COALESCE(memo, '')) LIKE :kw ESCAPE '\\')
                  UNION ALL
                  SELECT income_id FROM incomes
                  WHERE account_book_id = :bookId::uuid
                    AND (LOWER(COALESCE(source, '')) LIKE :kw ESCAPE '\\' OR LOWER(COALESCE(description, '')) LIKE :kw ESCAPE '\\')
                ) t
                """;

        long totalCount = ((Number) entityManager.createNativeQuery(countSql)
                .setParameter("bookId", bookId)
                .setParameter("kw", likeKeyword)
                .getSingleResult()).longValue();

        // 데이터: UNION ALL → 날짜 역순 → DB LIMIT/OFFSET
        String dataSql = """
                SELECT id, type, amount, title, category, memo, date FROM (
                  SELECT CAST(expense_id AS VARCHAR) AS id,
                         'EXPENSE'                  AS type,
                         amount,
                         COALESCE(merchant, category) AS title,
                         category,
                         memo,
                         date
                  FROM expenses
                  WHERE account_book_id = :bookId::uuid
                    AND (LOWER(COALESCE(merchant, '')) LIKE :kw ESCAPE '\\' OR LOWER(COALESCE(memo, '')) LIKE :kw ESCAPE '\\')
                  UNION ALL
                  SELECT CAST(income_id AS VARCHAR)      AS id,
                         'INCOME'                        AS type,
                         amount,
                         COALESCE(description, source)   AS title,
                         source                          AS category,
                         NULL                            AS memo,
                         date
                  FROM incomes
                  WHERE account_book_id = :bookId::uuid
                    AND (LOWER(COALESCE(source, '')) LIKE :kw ESCAPE '\\' OR LOWER(COALESCE(description, '')) LIKE :kw ESCAPE '\\')
                ) t
                ORDER BY t.date DESC
                LIMIT :size OFFSET :offset
                """;

        List<Object[]> rows = entityManager.createNativeQuery(dataSql)
                .setParameter("bookId", bookId)
                .setParameter("kw", likeKeyword)
                .setParameter("size", size)
                .setParameter("offset", (long) page * size)
                .getResultList();

        List<TransactionDto> transactions = rows.stream().map(row -> TransactionDto.builder()
                .id((String) row[0])
                .type((String) row[1])
                .amount(((Number) row[2]).longValue())
                .title((String) row[3])
                .category((String) row[4])
                .memo((String) row[5])
                .date(row[6].toString()) // java.sql.Date 또는 LocalDate 모두 "yyyy-MM-dd" 반환
                .time("")
                .build()).toList();

        boolean hasNext = (long) page * size + size < totalCount;

        return SearchResponse.builder()
                .transactions(transactions)
                .totalCount((int) totalCount)
                .hasNext(hasNext)
                .build();
    }

    /**
     * LIKE 패턴에 사용되는 특수문자를 이스케이프합니다.
     * \ 를 먼저 처리해야 이중 이스케이프를 방지할 수 있습니다.
     */
    static String escapeLikeKeyword(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
