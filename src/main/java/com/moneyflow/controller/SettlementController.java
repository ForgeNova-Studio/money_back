package com.moneyflow.controller;

import com.moneyflow.dto.request.BulkExpenseTransferRequest;
import com.moneyflow.dto.request.ExpenseTransferRequest;
import com.moneyflow.dto.response.ExpenseResponse;
import com.moneyflow.dto.response.SettlementResponse;
import com.moneyflow.service.ExpenseTransferService;
import com.moneyflow.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 정산 API 컨트롤러
 *
 * 여행 가계부 등의 N빵 정산 및 지출 이동 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Tag(name = "Settlement", description = "정산 관련 API")
public class SettlementController {

    private final SettlementService settlementService;
    private final ExpenseTransferService expenseTransferService;

    /**
     * 정산 계산
     *
     * 장부의 공용 지출에 대한 N빵 정산 결과를 계산합니다.
     */
    @GetMapping("/account-books/{accountBookId}")
    @Operation(summary = "정산 계산", description = "장부의 공용 지출에 대한 N빵 정산 결과를 계산합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정산 계산 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (장부 멤버가 아님)"),
            @ApiResponse(responseCode = "404", description = "장부를 찾을 수 없습니다")
    })
    public ResponseEntity<SettlementResponse> calculateSettlement(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID accountBookId) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        SettlementResponse response = settlementService.calculateSettlement(userId, accountBookId);
        return ResponseEntity.ok(response);
    }

    /**
     * 단건 지출 이동
     *
     * 개인 지출을 다른 장부로 이동합니다.
     */
    @PostMapping("/expenses/{expenseId}/transfer")
    @Operation(summary = "지출 이동", description = "개인 지출을 다른 장부로 이동합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지출 이동 성공"),
            @ApiResponse(responseCode = "400", description = "공용 지출은 이동할 수 없습니다"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인 지출이 아님)"),
            @ApiResponse(responseCode = "404", description = "지출 또는 대상 장부를 찾을 수 없습니다")
    })
    public ResponseEntity<ExpenseResponse> transferExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID expenseId,
            @Valid @RequestBody ExpenseTransferRequest request) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpenseResponse response = expenseTransferService.transferExpense(
                userId, expenseId, request.getTargetAccountBookId());
        return ResponseEntity.ok(response);
    }

    /**
     * 일괄 지출 이동
     *
     * 원본 장부의 본인 개인 지출을 모두 대상 장부로 이동합니다.
     */
    @PostMapping("/expenses/bulk-transfer")
    @Operation(summary = "일괄 지출 이동", description = "원본 장부의 본인 개인 지출을 모두 대상 장부로 이동합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일괄 이동 성공 (이동된 지출 개수 포함)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (장부 멤버가 아님)"),
            @ApiResponse(responseCode = "404", description = "원본 또는 대상 장부를 찾을 수 없습니다")
    })
    public ResponseEntity<ExpenseTransferService.BulkTransferResult> bulkTransferExpenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BulkExpenseTransferRequest request) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpenseTransferService.BulkTransferResult result = expenseTransferService.transferAllPersonalExpenses(
                userId, request.getSourceAccountBookId(), request.getTargetAccountBookId());
        return ResponseEntity.ok(result);
    }
}
