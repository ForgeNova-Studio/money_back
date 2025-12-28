package com.moneyflow.controller;

import com.moneyflow.dto.response.DailySummaryDto;
import com.moneyflow.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            @RequestParam String yearMonth // "2025-12"
    ) {
        // yearMonth 파싱 (예: "2025-12" → year=2025, month=12)
        String[] parts = yearMonth.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);

        Map<String, DailySummaryDto> data = homeService.getMonthlyData(year, month);
        return ResponseEntity.ok(data);
    }
}
