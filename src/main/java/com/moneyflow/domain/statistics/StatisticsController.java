package com.moneyflow.domain.statistics;

import com.moneyflow.dto.response.MonthlyStatisticsResponse;
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
}
