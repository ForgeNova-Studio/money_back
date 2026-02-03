package com.moneyflow.domain.statistics;

import com.moneyflow.dto.response.CategoryMonthlyComparisonResponse;
import com.moneyflow.dto.response.MonthlyStatisticsResponse;
import com.moneyflow.dto.response.TotalAssetResponse;
import com.moneyflow.dto.response.WeeklyStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 통계 API 컨트롤러
 *
 * 기능:
 * - 월간 통계 조회 (카테고리별 지출, 전월 대비)
 * - 주간 통계 조회 (일별 지출, 최다 카테고리, 일평균)
 * - 자산 현황 조회 (총자산, 기간별 손익)
 * - 장부별 개별 통계 지원
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "통계 API")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 월간 통계 조회
     */
    @GetMapping("/monthly")
    @Operation(summary = "월간 통계 조회", description = "특정 장부의 월간 통계를 조회합니다")
    public ResponseEntity<MonthlyStatisticsResponse> getMonthlyStatistics(
            @Parameter(description = "년도", example = "2026") @RequestParam int year,
            @Parameter(description = "월 (1-12)", example = "1") @RequestParam int month,
            @Parameter(description = "장부 ID (null이면 기본 장부)") @RequestParam(required = false) UUID accountBookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(userId, year, month, accountBookId);
        return ResponseEntity.ok(response);
    }

    /**
     * 카테고리별 전월 대비 변화 조회
     */
    @GetMapping("/monthly/category-comparison")
    @Operation(summary = "카테고리별 전월 대비 변화 조회", description = "각 지출 카테고리별로 전월 대비 증감을 조회합니다")
    public ResponseEntity<CategoryMonthlyComparisonResponse> getCategoryMonthlyComparison(
            @Parameter(description = "년도", example = "2026") @RequestParam int year,
            @Parameter(description = "월 (1-12)", example = "1") @RequestParam int month,
            @Parameter(description = "장부 ID (null이면 기본 장부)") @RequestParam(required = false) UUID accountBookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        CategoryMonthlyComparisonResponse response = statisticsService.getCategoryMonthlyComparison(userId, year, month, accountBookId);
        return ResponseEntity.ok(response);
    }

    /**
     * 주간 통계 조회
     */
    @GetMapping("/weekly")
    @Operation(summary = "주간 통계 조회", description = "특정 장부의 주간 통계를 조회합니다")
    public ResponseEntity<WeeklyStatisticsResponse> getWeeklyStatistics(
            @Parameter(description = "시작 날짜 (해당 날짜부터 7일)", example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "장부 ID (null이면 기본 장부)") @RequestParam(required = false) UUID accountBookId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        WeeklyStatisticsResponse response = statisticsService.getWeeklyStatistics(userId, startDate, accountBookId);
        return ResponseEntity.ok(response);
    }

    /**
     * 자산 현황 조회
     */
    @GetMapping("/assets")
    @Operation(
        summary = "자산 현황 조회",
        description = """
            사용자의 총자산과 기간별 손익을 조회합니다.

            **현재 자산**: 초기잔액 + 총수입 - 총지출 (날짜 필터 무관)
            **기간별 손익**: 선택한 기간 내의 수입/지출 집계

            **기간 기본값**:
            - startDate, endDate 모두 null: 이번 달 1일 ~ 오늘
            - startDate만 지정: startDate ~ 오늘
            - endDate만 지정: 이번 달 1일 ~ endDate
            - 둘 다 지정: startDate ~ endDate
            """
    )
    public ResponseEntity<TotalAssetResponse> getAssetStatistics(
            @Parameter(description = "장부 ID (null이면 기본 장부)")
            @RequestParam(required = false) UUID accountBookId,
            @Parameter(description = "조회 시작일 (yyyy-MM-dd, null이면 이번 달 1일)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "조회 종료일 (yyyy-MM-dd, null이면 오늘)", example = "2026-01-22")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "카테고리별 통계 포함 여부 (true: 수입 출처별/지출 카테고리별 통계 포함)", example = "false")
            @RequestParam(defaultValue = "false") boolean includeStats,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        // 기간 기본값 설정
        LocalDate now = LocalDate.now();
        if (startDate == null) {
            startDate = now.withDayOfMonth(1); // 이번 달 1일
        }
        if (endDate == null) {
            endDate = now; // 오늘
        }

        TotalAssetResponse response = statisticsService.getAssetStatistics(
            userId, accountBookId, startDate, endDate, includeStats
        );
        return ResponseEntity.ok(response);
    }
}
