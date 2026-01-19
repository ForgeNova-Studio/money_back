package com.moneyflow.domain.recurringexpense;

import com.moneyflow.dto.request.ConfirmMatchRequest;
import com.moneyflow.dto.response.MatchCandidateResponse;
import com.moneyflow.dto.response.RecurringExpensePaymentResponse;
import com.moneyflow.service.RecurringExpenseMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 고정비 결제 관리 API
 */
@RestController
@RequestMapping("/api/recurring-expenses/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recurring Expense Payment", description = "월별 고정비 결제 관리 API")
public class RecurringExpensePaymentController {

    private final RecurringExpenseMatchingService matchingService;

    @GetMapping
    @Operation(summary = "월별 결제 현황 조회", description = "해당 월의 모든 고정비 결제 상태를 조회합니다. PENDING은 자동 생성됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<List<RecurringExpensePaymentResponse>> getPaymentsForMonth(
            @Parameter(description = "년도", example = "2026") @RequestParam int year,
            @Parameter(description = "월", example = "1") @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<RecurringExpensePaymentResponse> payments = matchingService.getPaymentsForMonth(userId, year, month);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/match-candidates")
    @Operation(summary = "매칭 후보 조회", description = "특정 지출에 대해 매칭 가능한 고정비 후보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "지출을 찾을 수 없음")
    })
    public ResponseEntity<List<MatchCandidateResponse>> getMatchCandidates(
            @Parameter(description = "지출 ID", required = true) @RequestParam UUID expenseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<MatchCandidateResponse> candidates = matchingService.findMatchCandidates(expenseId);
        return ResponseEntity.ok(candidates);
    }

    @PostMapping("/{paymentId}/confirm")
    @Operation(summary = "매칭 확정", description = "지출과 고정비 결제를 매칭하여 확정합니다. 고정비 금액도 갱신됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "400", description = "이미 확정됨 또는 이미 연결된 지출"),
            @ApiResponse(responseCode = "404", description = "결제 또는 지출을 찾을 수 없음")
    })
    public ResponseEntity<RecurringExpensePaymentResponse> confirmMatch(
            @Parameter(description = "결제 ID", required = true) @PathVariable UUID paymentId,
            @Valid @RequestBody ConfirmMatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        RecurringExpensePaymentResponse response = matchingService.confirmMatch(paymentId, request.getExpenseId(),
                userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/skip")
    @Operation(summary = "이번 달 건너뛰기", description = "이번 달 결제를 건너뜁니다. (대출 상환 완료 등)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "건너뛰기 성공"),
            @ApiResponse(responseCode = "400", description = "PENDING 상태가 아님"),
            @ApiResponse(responseCode = "404", description = "결제를 찾을 수 없음")
    })
    public ResponseEntity<RecurringExpensePaymentResponse> skipPayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable UUID paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        RecurringExpensePaymentResponse response = matchingService.skipPayment(paymentId, userId);
        return ResponseEntity.ok(response);
    }
}
