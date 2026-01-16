package com.moneyflow.domain.income;

import com.moneyflow.domain.accountbook.AccountBook;
import com.moneyflow.domain.accountbook.AccountBookRepository;
import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.IncomeRequest;
import com.moneyflow.dto.response.IncomeListResponse;
import com.moneyflow.dto.response.IncomeResponse;
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
 * 수입 서비스
 *
 * 수입 관련 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncomeService {

        private final IncomeRepository incomeRepository;
        private final UserRepository userRepository;
        private final AccountBookRepository accountBookRepository;

        /**
         * 수입 생성
         *
         * @param userId  사용자 ID
         * @param request 수입 생성 요청
         * @return 생성된 수입 응답
         */
        @Transactional
        public IncomeResponse createIncome(UUID userId, IncomeRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

                AccountBook accountBook = null;
                if (request.getAccountBookId() != null) {
                        accountBook = accountBookRepository.findById(request.getAccountBookId())
                                        .orElseThrow(() -> new ResourceNotFoundException("장부를 찾을 수 없습니다"));

                        boolean isMember = accountBook.getMembers().stream()
                                        .anyMatch(member -> member.getUser().getUserId().equals(userId));

                        if (!isMember) {
                                throw new UnauthorizedException("해당 장부에 접근할 권한이 없습니다");
                        }
                }

                Income income = Income.builder()
                                .user(user)
                                .accountBook(accountBook)
                                .fundingSource(request.getFundingSource())
                                .amount(request.getAmount())
                                .date(request.getDate())
                                .source(request.getSource())
                                .description(request.getDescription())
                                .build();

                Income savedIncome = incomeRepository.save(income);
                log.info("Created income: {}", savedIncome.getIncomeId());

                return toResponse(savedIncome);
        }

        /**
         * 수입 목록 조회
         *
         * @param userId    사용자 ID
         * @param startDate 시작 날짜
         * @param endDate   종료 날짜
         * @param source    수입 출처 (null이면 전체 조회)
         * @return 수입 목록 응답
         */
        @Transactional(readOnly = true)
        public IncomeListResponse getIncomes(UUID userId, LocalDate startDate, LocalDate endDate, String source) {
                List<Income> incomes = incomeRepository.findIncomesByUserAndDateRange(
                                userId, startDate, endDate, source);

                List<IncomeResponse> incomeResponses = incomes.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                BigDecimal totalAmount = incomes.stream()
                                .map(Income::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return IncomeListResponse.builder()
                                .incomes(incomeResponses)
                                .totalAmount(totalAmount)
                                .count(incomes.size())
                                .build();
        }

        /**
         * 수입 상세 조회
         *
         * @param userId   사용자 ID
         * @param incomeId 수입 ID
         * @return 수입 응답
         */
        @Transactional(readOnly = true)
        public IncomeResponse getIncome(UUID userId, UUID incomeId) {
                Income income = incomeRepository.findById(incomeId)
                                .orElseThrow(() -> new ResourceNotFoundException("수입 내역을 찾을 수 없습니다"));

                if (!income.getUser().getUserId().equals(userId)) {
                        throw new UnauthorizedException("해당 수입 내역에 접근할 권한이 없습니다");
                }

                return toResponse(income);
        }

        /**
         * 수입 수정
         *
         * @param userId   사용자 ID
         * @param incomeId 수입 ID
         * @param request  수입 수정 요청
         * @return 수정된 수입 응답
         */
        @Transactional
        public IncomeResponse updateIncome(UUID userId, UUID incomeId, IncomeRequest request) {
                Income income = incomeRepository.findById(incomeId)
                                .orElseThrow(() -> new ResourceNotFoundException("수입 내역을 찾을 수 없습니다"));

                if (!income.getUser().getUserId().equals(userId)) {
                        throw new UnauthorizedException("해당 수입 내역을 수정할 권한이 없습니다");
                }

                // 업데이트
                income.setAmount(request.getAmount());
                income.setDate(request.getDate());
                income.setSource(request.getSource());
                income.setDescription(request.getDescription());

                Income updatedIncome = incomeRepository.save(income);
                log.info("Updated income: {}", incomeId);

                return toResponse(updatedIncome);
        }

        /**
         * 수입 삭제
         *
         * @param userId   사용자 ID
         * @param incomeId 수입 ID
         */
        @Transactional
        public void deleteIncome(UUID userId, UUID incomeId) {
                Income income = incomeRepository.findById(incomeId)
                                .orElseThrow(() -> new ResourceNotFoundException("수입 내역을 찾을 수 없습니다"));

                if (!income.getUser().getUserId().equals(userId)) {
                        throw new UnauthorizedException("해당 수입 내역을 삭제할 권한이 없습니다");
                }

                incomeRepository.delete(income);
                log.info("Deleted income: {}", incomeId);
        }

        /**
         * 최근 수입 내역 조회 (홈 화면용)
         *
         * @param userId 사용자 ID
         * @return 최근 수입 목록 (최대 5개)
         */
        @Transactional(readOnly = true)
        public List<IncomeResponse> getRecentIncomes(UUID userId) {
                List<Income> incomes = incomeRepository.findTop5ByUserUserIdOrderByDateDescCreatedAtDesc(userId);

                return incomes.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        /**
         * Entity를 Response DTO로 변환
         *
         * @param income 수입 엔티티
         * @return 수입 응답 DTO
         */
        private IncomeResponse toResponse(Income income) {
                return IncomeResponse.builder()
                                .incomeId(income.getIncomeId())
                                .userId(income.getUser().getUserId())
                                .accountBookId(income.getAccountBook() != null
                                                ? income.getAccountBook().getAccountBookId()
                                                : null)
                                .fundingSource(income.getFundingSource() != null
                                                ? income.getFundingSource().name()
                                                : null)
                                .amount(income.getAmount())
                                .date(income.getDate())
                                .source(income.getSource())
                                .description(income.getDescription())
                                .createdAt(income.getCreatedAt())
                                .updatedAt(income.getUpdatedAt())
                                .build();
        }
}
