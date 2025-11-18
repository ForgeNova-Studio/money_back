package com.moneyflow.domain.income;

import com.moneyflow.dto.request.IncomeRequest;
import com.moneyflow.dto.response.IncomeListResponse;
import com.moneyflow.dto.response.IncomeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 수입 컨트롤러
 *
 * 수입 관리 REST API 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Income", description = "수입 관리 API")
public class IncomeController {

    private final IncomeService incomeService;

    /**
     * 수입 생성
     *
     * @param request 수입 생성 요청
     * @param userDetails 인증된 사용자 정보
     * @return 생성된 수입 응답
     */
    @PostMapping
    @Operation(summary = "수입 생성", description = "새로운 수입을 등록합니다.")
    public ResponseEntity<IncomeResponse> createIncome(
            @Valid @RequestBody IncomeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        IncomeResponse response = incomeService.createIncome(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 수입 목록 조회
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param source 수입 출처 (선택)
     * @param userDetails 인증된 사용자 정보
     * @return 수입 목록 응답
     */
    @GetMapping
    @Operation(summary = "수입 목록 조회", description = "기간별 수입 목록을 조회합니다. 수입 출처로 필터링할 수 있습니다.")
    public ResponseEntity<IncomeListResponse> getIncomes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String source,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        IncomeListResponse response = incomeService.getIncomes(userId, startDate, endDate, source);
        return ResponseEntity.ok(response);
    }

    /**
     * 최근 수입 내역 조회 (홈 화면용)
     *
     * @param userDetails 인증된 사용자 정보
     * @return 최근 수입 목록 (최대 5개)
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 수입 내역 조회", description = "최근 수입 내역 5개를 조회합니다 (홈 화면용).")
    public ResponseEntity<List<IncomeResponse>> getRecentIncomes(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<IncomeResponse> response = incomeService.getRecentIncomes(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 수입 상세 조회
     *
     * @param incomeId 수입 ID
     * @param userDetails 인증된 사용자 정보
     * @return 수입 상세 응답
     */
    @GetMapping("/{incomeId}")
    @Operation(summary = "수입 상세 조회", description = "특정 수입의 상세 정보를 조회합니다.")
    public ResponseEntity<IncomeResponse> getIncome(
            @PathVariable UUID incomeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        IncomeResponse response = incomeService.getIncome(userId, incomeId);
        return ResponseEntity.ok(response);
    }

    /**
     * 수입 수정
     *
     * @param incomeId 수입 ID
     * @param request 수입 수정 요청
     * @param userDetails 인증된 사용자 정보
     * @return 수정된 수입 응답
     */
    @PutMapping("/{incomeId}")
    @Operation(summary = "수입 수정", description = "기존 수입 정보를 수정합니다.")
    public ResponseEntity<IncomeResponse> updateIncome(
            @PathVariable UUID incomeId,
            @Valid @RequestBody IncomeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        IncomeResponse response = incomeService.updateIncome(userId, incomeId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 수입 삭제
     *
     * @param incomeId 수입 ID
     * @param userDetails 인증된 사용자 정보
     * @return 204 No Content
     */
    @DeleteMapping("/{incomeId}")
    @Operation(summary = "수입 삭제", description = "특정 수입을 삭제합니다.")
    public ResponseEntity<Void> deleteIncome(
            @PathVariable UUID incomeId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        incomeService.deleteIncome(userId, incomeId);
        return ResponseEntity.noContent().build();
    }
}
