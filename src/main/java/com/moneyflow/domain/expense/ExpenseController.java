package com.moneyflow.domain.expense;

import com.moneyflow.dto.request.ExpenseRequest;
import com.moneyflow.dto.response.ExpenseListResponse;
import com.moneyflow.dto.response.ExpenseResponse;
import com.moneyflow.dto.response.OcrResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Expense", description = "지출 관리 API")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final OcrService ocrService;

    @PostMapping
    @Operation(summary = "지출 생성")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpenseResponse response = expenseService.createExpense(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "지출 목록 조회")
    public ResponseEntity<ExpenseListResponse> getExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpenseListResponse response = expenseService.getExpenses(userId, startDate, endDate, category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 지출 내역 조회 (홈 화면용)")
    public ResponseEntity<List<ExpenseResponse>> getRecentExpenses(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        List<ExpenseResponse> response = expenseService.getRecentExpenses(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "지출 상세 조회")
    public ResponseEntity<ExpenseResponse> getExpense(
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpenseResponse response = expenseService.getExpense(userId, expenseId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{expenseId}")
    @Operation(summary = "지출 수정")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID expenseId,
            @Valid @RequestBody ExpenseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        ExpenseResponse response = expenseService.updateExpense(userId, expenseId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{expenseId}")
    @Operation(summary = "지출 삭제")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        expenseService.deleteExpense(userId, expenseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/ocr",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "OCR 이미지 처리",
        description = "영수증/결제 알림 이미지를 업로드하여 금액, 날짜, 가맹점 정보를 자동으로 추출합니다. " +
                      "현재는 Mock 데이터를 반환하며, 추후 Google Cloud Vision API로 교체 예정입니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "OCR 처리 성공",
            content = @Content(schema = @Schema(implementation = OcrResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (파일 형식 또는 크기 초과)"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<OcrResponse> processOcr(
            @Parameter(
                    description = "업로드할 이미지 파일 (JPG, PNG)",
                    required = true,
                    schema = @Schema(type = "string", format = "binary")
            )
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 1. 파일 존재 확인
        if (image == null || image.isEmpty()) {
            throw new BusinessException("이미지 파일이 비어있습니다");
        }

        // 2. 파일 크기 제한 (5MB)
        if (image.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("이미지 크기는 5MB 이하여야 합니다");
        }

        // 3. 파일 형식 검증 (JPG, PNG만 허용)
        String contentType = image.getContentType();
        if (contentType == null ||
            (!contentType.equals("image/jpeg") &&
             !contentType.equals("image/png") &&
             !contentType.equals("image/jpg"))) {
            throw new BusinessException("이미지 형식은 JPG 또는 PNG만 지원합니다");
        }

        // 4. OCR 처리
        OcrResponse response = ocrService.processImage(image);
        log.info("OCR processed for user: {}, filename: {}, size: {} bytes",
                 userDetails.getUsername(), image.getOriginalFilename(), image.getSize());

        return ResponseEntity.ok(response);
    }
}
