package com.moneyflow.service;

import com.moneyflow.domain.accountbook.AccountBookMemberRepository;
import com.moneyflow.domain.expense.ExpenseRepository;
import com.moneyflow.domain.income.IncomeRepository;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private IncomeRepository incomeRepository;
    @Mock
    private AccountBookMemberRepository accountBookMemberRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private HomeService homeService;

    @BeforeEach
    void injectEntityManager() {
        // @PersistenceContext는 Mockito @InjectMocks가 주입하지 않으므로 직접 주입
        ReflectionTestUtils.setField(homeService, "entityManager", entityManager);
    }

    // ===== escapeLikeKeyword 단위 테스트 =====

    @Nested
    @DisplayName("escapeLikeKeyword")
    class EscapeLikeKeyword {

        @Test
        @DisplayName("% 문자를 \\%로 이스케이프한다")
        void escapes_percent() {
            assertThat(HomeService.escapeLikeKeyword("100%할인")).isEqualTo("100\\%할인");
        }

        @Test
        @DisplayName("_ 문자를 \\_ 로 이스케이프한다")
        void escapes_underscore() {
            assertThat(HomeService.escapeLikeKeyword("카_페")).isEqualTo("카\\_페");
        }

        @Test
        @DisplayName("\\ 문자를 먼저 이스케이프해 이중 처리를 방지한다")
        void escapes_backslash_first_to_prevent_double_escape() {
            // \% 입력 시 → \\% (백슬래시 이스케이프) → 그 다음 % 이스케이프 대상이 아님
            assertThat(HomeService.escapeLikeKeyword("\\%")).isEqualTo("\\\\\\%");
        }

        @Test
        @DisplayName("특수문자 없는 일반 키워드는 그대로 반환한다")
        void normal_keyword_unchanged() {
            assertThat(HomeService.escapeLikeKeyword("스타벅스")).isEqualTo("스타벅스");
        }

        @Test
        @DisplayName("%, _, \\ 모두 포함된 키워드를 올바르게 이스케이프한다")
        void escapes_all_special_chars() {
            assertThat(HomeService.escapeLikeKeyword("a%b_c\\d")).isEqualTo("a\\%b\\_c\\\\d");
        }
    }

    // ===== searchTransactions 단위 테스트 =====

    @Nested
    @DisplayName("searchTransactions")
    class SearchTransactions {

        @Test
        @DisplayName("장부 접근 권한이 없으면 ACCOUNT_BOOK_ACCESS_DENIED 예외가 발생한다")
        void throwsWhenNotMember() {
            UUID userId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();

            when(accountBookMemberRepository
                    .existsByAccountBookAccountBookIdAndUserUserId(bookId, userId))
                    .thenReturn(false);

            assertThatThrownBy(() -> homeService.searchTransactions(userId, bookId, "스타벅스", 0, 20))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ACCOUNT_BOOK_ACCESS_DENIED));
        }

        @Test
        @DisplayName("% 키워드 검색 시 이스케이프 처리 후 결과를 반환한다")
        void percentKeyword_escapedAndReturnsResult() {
            UUID userId = UUID.randomUUID();
            UUID bookId = UUID.randomUUID();

            when(accountBookMemberRepository
                    .existsByAccountBookAccountBookIdAndUserUserId(bookId, userId))
                    .thenReturn(true);

            // count query mock
            Query countQuery = mock(Query.class);
            when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery);
            when(countQuery.setParameter(anyString(), anyString())).thenReturn(countQuery);
            when(countQuery.getSingleResult()).thenReturn(0L);

            // data query mock
            Query dataQuery = mock(Query.class);
            when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery).thenReturn(dataQuery);
            when(dataQuery.setParameter(anyString(), anyString())).thenReturn(dataQuery);
            when(dataQuery.setParameter(anyString(), anyInt())).thenReturn(dataQuery);
            when(dataQuery.setParameter(anyString(), anyLong())).thenReturn(dataQuery);
            when(dataQuery.getResultList()).thenReturn(List.of());

            // % 키워드도 예외 없이 처리되어야 함
            var result = homeService.searchTransactions(userId, bookId, "%", 0, 20);
            assertThat(result.getTransactions()).isEmpty();
            assertThat(result.getTotalCount()).isZero();
        }
    }

    private long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    private int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
