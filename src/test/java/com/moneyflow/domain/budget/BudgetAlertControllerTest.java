package com.moneyflow.domain.budget;

import com.moneyflow.dto.response.BudgetAlertResponse;
import com.moneyflow.exception.GlobalExceptionHandler;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BudgetAlertControllerTest {

    @Mock
    private BudgetAlertService budgetAlertService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BudgetAlertController controller = new BudgetAlertController(budgetAlertService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────── GET /api/budget-alerts ────────────────────

    @Test
    @DisplayName("예산 알림 목록 조회: 정상 요청 → 200 + 알림 목록 반환")
    void getAlerts_validRequest_returns200WithAlerts() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();

        BudgetAlertResponse alert = BudgetAlertResponse.builder()
                .alertId(alertId)
                .accountBookId(accountBookId)
                .year(2026)
                .month(3)
                .thresholdType(ThresholdType.WARNING_80)
                .triggeredAt(LocalDateTime.of(2026, 3, 15, 10, 0))
                .build();

        when(budgetAlertService.getAlerts(userId, accountBookId, 2026, 3))
                .thenReturn(List.of(alert));

        mockMvc.perform(get("/api/budget-alerts")
                        .with(authenticatedUser(userId))
                        .param("accountBookId", accountBookId.toString())
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alertId").value(alertId.toString()))
                .andExpect(jsonPath("$[0].thresholdType").value("WARNING_80"))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].month").value(3));
    }

    @Test
    @DisplayName("예산 알림 목록 조회: 알림 없을 때 → 200 + 빈 배열")
    void getAlerts_noAlerts_returns200WithEmptyArray() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();

        when(budgetAlertService.getAlerts(userId, accountBookId, 2026, 3))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/budget-alerts")
                        .with(authenticatedUser(userId))
                        .param("accountBookId", accountBookId.toString())
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("예산 알림 목록 조회: accountBookId 파라미터 누락 → 400")
    void getAlerts_missingAccountBookId_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/budget-alerts")
                        .with(authenticatedUser(userId))
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예산 알림 목록 조회: year 파라미터 누락 → 400")
    void getAlerts_missingYear_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();

        mockMvc.perform(get("/api/budget-alerts")
                        .with(authenticatedUser(userId))
                        .param("accountBookId", accountBookId.toString())
                        .param("month", "3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("예산 알림 목록 조회: EXCEEDED_100 알림 포함 → thresholdType 필드 정확히 반환")
    void getAlerts_exceededAlert_returnsCorrectThresholdType() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();

        BudgetAlertResponse exceededAlert = BudgetAlertResponse.builder()
                .alertId(UUID.randomUUID())
                .accountBookId(accountBookId)
                .year(2026)
                .month(3)
                .thresholdType(ThresholdType.EXCEEDED_100)
                .triggeredAt(LocalDateTime.of(2026, 3, 25, 14, 30))
                .build();

        when(budgetAlertService.getAlerts(userId, accountBookId, 2026, 3))
                .thenReturn(List.of(exceededAlert));

        mockMvc.perform(get("/api/budget-alerts")
                        .with(authenticatedUser(userId))
                        .param("accountBookId", accountBookId.toString())
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].thresholdType").value("EXCEEDED_100"));
    }

    @Test
    @DisplayName("예산 알림 목록 조회: 같은 달 WARNING_80 + EXCEEDED_100 두 개 → 모두 반환")
    void getAlerts_multipleAlertsInSameMonth_returnsAll() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID accountBookId = UUID.randomUUID();

        BudgetAlertResponse warning = BudgetAlertResponse.builder()
                .alertId(UUID.randomUUID())
                .accountBookId(accountBookId)
                .year(2026).month(3)
                .thresholdType(ThresholdType.WARNING_80)
                .triggeredAt(LocalDateTime.of(2026, 3, 20, 9, 0))
                .build();

        BudgetAlertResponse exceeded = BudgetAlertResponse.builder()
                .alertId(UUID.randomUUID())
                .accountBookId(accountBookId)
                .year(2026).month(3)
                .thresholdType(ThresholdType.EXCEEDED_100)
                .triggeredAt(LocalDateTime.of(2026, 3, 28, 15, 0))
                .build();

        when(budgetAlertService.getAlerts(userId, accountBookId, 2026, 3))
                .thenReturn(List.of(warning, exceeded));

        mockMvc.perform(get("/api/budget-alerts")
                        .with(authenticatedUser(userId))
                        .param("accountBookId", accountBookId.toString())
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ──────────────────── Helper ────────────────────

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
