package com.moneyflow.domain.recurringexpense;

import com.moneyflow.dto.request.CreateRecurringExpenseRequest;
import com.moneyflow.dto.request.UpdateRecurringExpenseRequest;
import com.moneyflow.dto.response.MonthlyRecurringTotalResponse;
import com.moneyflow.dto.response.RecurringExpenseResponse;
import com.moneyflow.service.RecurringExpenseService;
import com.moneyflow.service.SubscriptionDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 고정비 및 구독료 관리 컨트롤러
 */
@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
@Tag(name = "Recurring Expenses", description = "고정비 및 구독료 관리 API")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;
    private final SubscriptionDetectionService subscriptionDetectionService;

    @PostMapping
    @Operation(
            summary = "고정비 등록",
            description = "새로운 고정비 또는 구독료를 등록합니다. " +
                    "월세, 보험료, 구독료 등 반복적인 지출을 관리할 수 있습니다. " +
                    "반복 유형(MONTHLY, YEARLY, WEEKLY)과 다음 결제일을 설정할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "고정비 등록 성공",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 필드 누락, 유효하지 않은 값 등)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (JWT 토큰 없음 또는 만료)"
            )
    })
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateRecurringExpenseRequest request) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        RecurringExpenseResponse response = recurringExpenseService
                .createRecurringExpense(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "고정비 목록 조회",
            description = "사용자의 모든 고정비 목록을 조회합니다. " +
                    "includeCouple=true로 설정하면 커플 모드에서 공동 고정비도 함께 조회됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    public ResponseEntity<List<RecurringExpenseResponse>> getRecurringExpenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "커플 고정비 포함 여부", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean includeCouple) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<RecurringExpenseResponse> responses = recurringExpenseService
                .getRecurringExpenses(userId, includeCouple);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    @Operation(
            summary = "활성 고정비 목록 조회",
            description = "종료되지 않은 고정비 목록만 조회합니다. " +
                    "endDate가 null이거나 현재 날짜 이후인 항목만 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    public ResponseEntity<List<RecurringExpenseResponse>> getActiveRecurringExpenses(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "커플 고정비 포함 여부", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean includeCouple) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<RecurringExpenseResponse> responses = recurringExpenseService
                .getActiveRecurringExpenses(userId, includeCouple);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/subscriptions")
    @Operation(
            summary = "구독료만 조회",
            description = "isSubscription=true로 설정된 항목만 조회합니다. " +
                    "넷플릭스, 스포티파이 등 구독 서비스만 필터링하여 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    public ResponseEntity<List<RecurringExpenseResponse>> getSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<RecurringExpenseResponse> responses = recurringExpenseService
                .getSubscriptions(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "다가오는 결제 조회",
            description = "지정된 일수 내에 결제 예정인 고정비 목록을 조회합니다. " +
                    "daysAhead를 지정하지 않으면 기본 7일 이내의 결제를 조회합니다. " +
                    "다음 결제일이 가까운 순서로 정렬되어 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    public ResponseEntity<List<RecurringExpenseResponse>> getUpcomingPayments(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "조회할 일수 (기본: 7일)", example = "7")
            @RequestParam(required = false) Integer daysAhead,
            @Parameter(description = "커플 고정비 포함 여부", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean includeCouple) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<RecurringExpenseResponse> responses = recurringExpenseService
                .getUpcomingPayments(userId, daysAhead, includeCouple);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{recurringExpenseId}")
    @Operation(
            summary = "고정비 상세 조회",
            description = "특정 고정비의 상세 정보를 조회합니다. " +
                    "본인의 고정비이거나 커플 고정비인 경우에만 조회 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (다른 사용자의 고정비)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "고정비를 찾을 수 없음"
            )
    })
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "고정비 ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID recurringExpenseId) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        RecurringExpenseResponse response = recurringExpenseService
                .getRecurringExpense(userId, recurringExpenseId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{recurringExpenseId}")
    @Operation(
            summary = "고정비 수정",
            description = "기존 고정비 정보를 수정합니다. " +
                    "금액이 변경되면 lastAmount 필드에 이전 금액이 저장되어 변동 감지가 가능합니다. " +
                    "본인의 고정비만 수정 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (다른 사용자의 고정비)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "고정비를 찾을 수 없음"
            )
    })
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "고정비 ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID recurringExpenseId,
            @Valid @RequestBody UpdateRecurringExpenseRequest request) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        RecurringExpenseResponse response = recurringExpenseService
                .updateRecurringExpense(userId, recurringExpenseId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{recurringExpenseId}")
    @Operation(
            summary = "고정비 삭제",
            description = "고정비를 삭제합니다. 본인의 고정비만 삭제 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (다른 사용자의 고정비)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "고정비를 찾을 수 없음"
            )
    })
    public ResponseEntity<Void> deleteRecurringExpense(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "고정비 ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID recurringExpenseId) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        recurringExpenseService.deleteRecurringExpense(userId, recurringExpenseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/monthly-total")
    @Operation(
            summary = "월간 고정비 총액 조회",
            description = "MONTHLY 유형의 활성 고정비 총액을 계산하여 반환합니다. " +
                    "매달 나가는 고정 지출 금액을 한눈에 확인할 수 있습니다. " +
                    "includeCouple=true로 설정하면 커플 고정비도 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MonthlyRecurringTotalResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    public ResponseEntity<MonthlyRecurringTotalResponse> getMonthlyTotal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "커플 고정비 포함 여부", example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean includeCouple) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        MonthlyRecurringTotalResponse response = recurringExpenseService
                .getMonthlyTotal(userId, includeCouple);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/detect-subscriptions")
    @Operation(
            summary = "구독료 자동 탐지",
            description = "사용자의 지출 내역을 분석하여 반복적인 패턴을 찾아 구독료로 자동 등록합니다. " +
                    "넷플릭스, 스포티파이 등 매달 같은 금액이 결제되는 항목을 탐지합니다. " +
                    "monthsToAnalyze를 지정하여 분석할 과거 개월 수를 설정할 수 있습니다 (기본: 3개월). " +
                    "최소 신뢰도 0.7 이상인 항목만 자동 등록됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "탐지 완료 (탐지된 구독 목록 반환)",
                    content = @Content(schema = @Schema(implementation = RecurringExpenseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    public ResponseEntity<List<RecurringExpenseResponse>> detectSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "분석할 과거 개월 수", example = "3")
            @RequestParam(required = false) Integer monthsToAnalyze) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<RecurringExpense> detectedSubscriptions = subscriptionDetectionService
                .detectSubscriptions(userId, monthsToAnalyze);

        List<RecurringExpenseResponse> responses = detectedSubscriptions.stream()
                .map(e -> RecurringExpenseResponse.builder()
                        .recurringExpenseId(e.getRecurringExpenseId())
                        .userId(e.getUser().getUserId())
                        .accountBookId(e.getAccountBook() != null ? e.getAccountBook().getAccountBookId() : null)
                        .name(e.getName())
                        .amount(e.getAmount())
                        .category(e.getCategory())
                        .description(e.getDescription())
                        .recurringType(e.getRecurringType())
                        .startDate(e.getStartDate())
                        .endDate(e.getEndDate())
                        .dayOfMonth(e.getDayOfMonth())
                        .dayOfWeek(e.getDayOfWeek())
                        .nextPaymentDate(e.getNextPaymentDate())
                        .isSubscription(e.getIsSubscription())
                        .subscriptionProvider(e.getSubscriptionProvider())
                        .notificationEnabled(e.getNotificationEnabled())
                        .lastAmount(e.getLastAmount())
                        .lastPaymentDate(e.getLastPaymentDate())
                        .autoDetected(e.getAutoDetected())
                        .detectionConfidence(e.getDetectionConfidence())
                        .createdAt(e.getCreatedAt())
                        .updatedAt(e.getUpdatedAt())
                        .hasAmountChanged(e.hasAmountChanged())
                        .isExpired(e.isExpired())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
