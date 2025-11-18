package com.moneyflow.domain.statistics;

import com.moneyflow.dto.response.MonthlyStatisticsResponse;
import com.moneyflow.dto.response.WeeklyStatisticsResponse;
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
 *
 * 엔드포인트:
 * - GET /api/statistics/monthly?year={year}&month={month}
 * - GET /api/statistics/weekly?startDate={startDate}
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 월간 통계 조회
     *
     * @param year 조회할 년도 (예: 2025)
     * @param month 조회할 월 (1-12)
     * @param userDetails 인증된 사용자 정보
     * @return 월간 통계 정보
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyStatisticsResponse> getMonthlyStatistics(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 사용자 ID 추출 (UserDetails의 username에 userId가 저장됨)
        UUID userId = UUID.fromString(userDetails.getUsername());

        // 월간 통계 조회
        MonthlyStatisticsResponse response = statisticsService.getMonthlyStatistics(userId, year, month);

        return ResponseEntity.ok(response);
    }

    /**
     * 주간 통계 조회
     *
     * @param startDate 시작 날짜 (해당 날짜부터 6일 후까지 총 7일)
     *                  형식: yyyy-MM-dd (예: 2025-11-11)
     * @param userDetails 인증된 사용자 정보
     * @return 주간 통계 정보
     */
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyStatisticsResponse> getWeeklyStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 사용자 ID 추출 (UserDetails의 username에 userId가 저장됨)
        UUID userId = UUID.fromString(userDetails.getUsername());

        // 주간 통계 조회
        WeeklyStatisticsResponse response = statisticsService.getWeeklyStatistics(userId, startDate);

        return ResponseEntity.ok(response);
    }
}
