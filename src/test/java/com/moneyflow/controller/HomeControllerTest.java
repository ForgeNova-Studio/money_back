package com.moneyflow.controller;

import com.moneyflow.dto.response.DailySummaryDto;
import com.moneyflow.dto.response.SearchResponse;
import com.moneyflow.dto.response.TransactionDto;
import com.moneyflow.exception.GlobalExceptionHandler;
import com.moneyflow.service.HomeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private HomeService homeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HomeController homeController = new HomeController(homeService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(homeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ===== GET /api/home/monthly-data =====

    @Test
    @DisplayName("월간 데이터 조회: 유효한 yearMonth → 200 및 서비스 호출")
    void getMonthlyData_validYearMonth_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        when(homeService.getMonthlyData(eq(userId), eq(bookId), eq(2026), eq(3)))
                .thenReturn(Map.of());

        mockMvc.perform(get("/api/home/monthly-data")
                        .with(authenticatedUser(userId))
                        .param("yearMonth", "2026-03")
                        .param("accountBookId", bookId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("월간 데이터 조회: 숫자만 있는 yearMonth('2026') → 400")
    void getMonthlyData_missingMonth_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(get("/api/home/monthly-data")
                        .with(authenticatedUser(userId))
                        .param("yearMonth", "2026")
                        .param("accountBookId", bookId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("월간 데이터 조회: 문자열 yearMonth('abc') → 400")
    void getMonthlyData_stringYearMonth_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(get("/api/home/monthly-data")
                        .with(authenticatedUser(userId))
                        .param("yearMonth", "abc")
                        .param("accountBookId", bookId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("월간 데이터 조회: 월이 범위 초과('2026-13') → 400")
    void getMonthlyData_invalidMonth_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        mockMvc.perform(get("/api/home/monthly-data")
                        .with(authenticatedUser(userId))
                        .param("yearMonth", "2026-13")
                        .param("accountBookId", bookId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    // ===== GET /api/home/search =====

    @Test
    @DisplayName("거래 검색: 정상 요청 → 200 및 SearchResponse 반환")
    void searchTransactions_validRequest_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        TransactionDto tx = TransactionDto.builder()
                .id(UUID.randomUUID().toString())
                .type("EXPENSE")
                .amount(5500L)
                .title("스타벅스")
                .category("CAFE_SNACK")
                .date("2026-03-15")
                .time("")
                .build();

        SearchResponse response = SearchResponse.builder()
                .transactions(List.of(tx))
                .totalCount(1)
                .hasNext(false)
                .build();

        when(homeService.searchTransactions(eq(userId), eq(bookId), eq("스타벅스"), eq(0), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/home/search")
                        .with(authenticatedUser(userId))
                        .param("keyword", "스타벅스")
                        .param("accountBookId", bookId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.transactions[0].title").value("스타벅스"))
                .andExpect(jsonPath("$.transactions[0].type").value("EXPENSE"));
    }

    @Test
    @DisplayName("거래 검색: page/size 기본값 0/20 적용 확인")
    void searchTransactions_defaultPagination_usesPageZeroSize20() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        SearchResponse emptyResponse = SearchResponse.builder()
                .transactions(List.of())
                .totalCount(0)
                .hasNext(false)
                .build();

        when(homeService.searchTransactions(any(), any(), eq("카페"), eq(0), eq(20)))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/home/search")
                        .with(authenticatedUser(userId))
                        .param("keyword", "카페")
                        .param("accountBookId", bookId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    // ===== Helper =====

    private RequestPostProcessor authenticatedUser(UUID userId) {
        return request -> {
            UserDetails principal = User.withUsername(userId.toString())
                    .password("")
                    .roles("USER")
                    .build();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return request;
        };
    }
}
