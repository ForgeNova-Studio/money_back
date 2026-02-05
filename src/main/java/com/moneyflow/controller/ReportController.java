package com.moneyflow.controller;

import com.moneyflow.dto.response.MonthlyReportResponse;
import com.moneyflow.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 리포트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "월간/연간 리포트 API")
public class ReportController {

    private final ReportService reportService;

    /**
     * 월간 리포트 조회
     * 
     * @param accountBookId 가계부 ID
     * @param year          연도
     * @param month         월
     * @return 월간 리포트
     */
    @GetMapping("/monthly")
    @Operation(summary = "월간 리포트 조회", description = "특정 월의 지출/수입 리포트를 조회합니다.")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam UUID accountBookId,
            @RequestParam int year,
            @RequestParam int month) {

        MonthlyReportResponse report = reportService.getMonthlyReport(accountBookId, year, month);
        return ResponseEntity.ok(report);
    }
}
