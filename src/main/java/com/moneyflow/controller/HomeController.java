package com.moneyflow.controller;

import com.moneyflow.dto.response.DailySummaryDto;
import com.moneyflow.dto.response.SearchResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "월간 데이터 조회", description = "특정 월의 일별 수입/지출 요약 및 상세 내역 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 yearMonth 형식"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/monthly-data")
    public ResponseEntity<Map<String, DailySummaryDto>> getMonthlyData(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String yearMonth, // "2025-12"
            @RequestParam UUID accountBookId
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        // yearMonth 파싱 (예: "2025-12") — 형식 오류 시 400 반환
        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new BusinessException("yearMonth 형식이 올바르지 않습니다 (예: 2025-12)", ErrorCode.INVALID_INPUT);
        }

        Map<String, DailySummaryDto> data =
                homeService.getMonthlyData(userId, accountBookId, ym.getYear(), ym.getMonthValue());
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "거래 내역 검색", description = "전체 기간의 지출/수입을 가맹점명·메모·출처·설명으로 검색 (대소문자 무관)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "keyword가 비어있거나 accountBookId 누락"),
            @ApiResponse(responseCode = "403", description = "장부 접근 권한 없음")
    })
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @NotBlank String keyword,
            @RequestParam UUID accountBookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        SearchResponse response = homeService.searchTransactions(userId, accountBookId, keyword, page, size);
        return ResponseEntity.ok(response);
    }
}
