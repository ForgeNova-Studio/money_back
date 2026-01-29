package com.moneyflow.domain.asset;

import com.moneyflow.dto.request.AssetRequest;
import com.moneyflow.dto.response.AssetResponse;
import com.moneyflow.dto.response.AssetSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Asset", description = "자산 관리 API")
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    @Operation(summary = "자산 생성", description = "새로운 자산(현금, 적금, 주식 등)을 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "자산 생성 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AssetResponse> createAsset(
            @Valid @RequestBody AssetRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        AssetResponse response = assetService.createAsset(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "자산 목록 조회", description = "사용자의 모든 자산을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<List<AssetResponse>> getAssets(
            @Parameter(description = "카테고리 필터 (선택)")
            @RequestParam(required = false) AssetCategory category,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());

        List<AssetResponse> response;
        if (category != null) {
            response = assetService.getAssetsByCategory(userId, category);
        } else {
            response = assetService.getAssets(userId);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "자산 요약 조회", description = "총 자산, 전월 대비 변화, 카테고리별 비율을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<AssetSummaryResponse> getAssetSummary(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        AssetSummaryResponse response = assetService.getAssetSummary(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{assetId}")
    @Operation(summary = "자산 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "자산을 찾을 수 없음")
    })
    public ResponseEntity<AssetResponse> getAsset(
            @PathVariable UUID assetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        AssetResponse response = assetService.getAsset(userId, assetId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{assetId}")
    @Operation(summary = "자산 수정", description = "기존 자산 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "자산을 찾을 수 없음")
    })
    public ResponseEntity<AssetResponse> updateAsset(
            @PathVariable UUID assetId,
            @Valid @RequestBody AssetRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        AssetResponse response = assetService.updateAsset(userId, assetId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{assetId}")
    @Operation(summary = "자산 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "자산을 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteAsset(
            @PathVariable UUID assetId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        assetService.deleteAsset(userId, assetId);
        return ResponseEntity.noContent().build();
    }
}
