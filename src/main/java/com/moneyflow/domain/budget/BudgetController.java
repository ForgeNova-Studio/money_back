package com.moneyflow.domain.budget;

import com.moneyflow.dto.request.BudgetRequest;
import com.moneyflow.dto.response.BudgetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 예산(목표 소비) 관리 API
 */
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Budget", description = "예산 관리 API")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    @Operation(summary = "예산 생성 또는 수정")
    public ResponseEntity<BudgetResponse> createOrUpdateBudget(
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        BudgetResponse response = budgetService.createOrUpdateBudget(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "특정 가계부의 특정 년월 예산 조회")
    public ResponseEntity<BudgetResponse> getBudget(
            @RequestParam UUID accountBookId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        BudgetResponse response = budgetService.getBudget(userId, accountBookId, year, month);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{budgetId}")
    @Operation(summary = "예산 삭제")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable UUID budgetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        budgetService.deleteBudget(userId, budgetId);
        return ResponseEntity.noContent().build();
    }
}
