package com.moneyflow.domain.budget;

import com.moneyflow.dto.response.BudgetAlertResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 예산 알림 조회 API
 */
@RestController
@RequestMapping("/api/budget-alerts")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "BudgetAlert", description = "예산 알림 API")
public class BudgetAlertController {

    private final BudgetAlertService budgetAlertService;

    @GetMapping
    @Operation(summary = "특정 가계부의 예산 알림 목록 조회")
    public ResponseEntity<List<BudgetAlertResponse>> getAlerts(
            @RequestParam UUID accountBookId,
            @RequestParam @Min(2000) @Max(2100) Integer year,
            @RequestParam @Min(1) @Max(12) Integer month,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        List<BudgetAlertResponse> alerts = budgetAlertService.getAlerts(userId, accountBookId, year, month);
        return ResponseEntity.ok(alerts);
    }
}
