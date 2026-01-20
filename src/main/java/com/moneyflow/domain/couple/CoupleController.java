package com.moneyflow.domain.couple;

import com.moneyflow.dto.request.JoinCoupleRequest;
import com.moneyflow.dto.response.CoupleResponse;
import com.moneyflow.dto.response.InviteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 커플 API 컨트롤러
 *
 * 기능:
 * - 초대 코드 생성
 * - 커플 가입 (초대 코드 입력)
 * - 커플 연동 해제
 * - 현재 커플 정보 조회
 *
 * 엔드포인트:
 * - POST /api/couples/invite - 초대 코드 생성
 * - POST /api/couples/join - 초대 코드로 가입
 * - DELETE /api/couples/unlink - 커플 연동 해제
 * - GET /api/couples/me - 현재 커플 정보 조회
 */
@RestController
@RequestMapping("/api/couples")
@RequiredArgsConstructor
@Tag(name = "Couple", description = "커플 관리 API")
public class CoupleController {

    private final CoupleService coupleService;

    /**
     * 초대 코드 생성
     *
     * 사용자가 파트너를 초대하기 위한 초대 코드를 생성합니다.
     * 기존에 생성된 유효한 초대 코드가 있으면 재사용합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 초대 코드 정보
     */
    @PostMapping("/invite")
    @Operation(
            summary = "초대 코드 생성",
            description = "파트너를 초대하기 위한 초대 코드를 생성합니다. 기존에 생성된 유효한 초대 코드가 있으면 재사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 코드 생성 성공"),
            @ApiResponse(responseCode = "400", description = "이미 커플 상태입니다"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<InviteResponse> generateInviteCode(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        InviteResponse response = coupleService.generateInviteCode(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 커플 가입 (초대 코드 입력)
     *
     * 초대 코드를 입력하여 파트너의 커플에 가입합니다.
     *
     * @param request     가입 요청 (초대 코드 포함)
     * @param userDetails 인증된 사용자 정보
     * @return 커플 정보
     */
    @PostMapping("/join")
    @Operation(
            summary = "커플 가입",
            description = "초대 코드를 입력하여 파트너의 커플에 가입합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "커플 가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 초대 코드 또는 이미 커플 상태입니다"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "초대 코드를 찾을 수 없습니다")
    })
    public ResponseEntity<CoupleResponse> joinCouple(
            @Valid @RequestBody JoinCoupleRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        CoupleResponse response = coupleService.joinCouple(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 커플 연동 해제
     *
     * 현재 연동된 커플을 해제합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 성공 메시지
     */
    @DeleteMapping("/unlink")
    @Operation(
            summary = "커플 연동 해제",
            description = "현재 연동된 커플을 해제합니다. 두 사용자 모두 개인 모드로 전환됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "커플 연동 해제 성공"),
            @ApiResponse(responseCode = "400", description = "커플 상태가 아닙니다"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Map<String, String>> unlinkCouple(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        coupleService.unlinkCouple(userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "커플 연동이 해제되었습니다");
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 커플 정보 조회
     *
     * 사용자가 현재 속해있는 커플 정보를 조회합니다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 커플 정보 (없으면 404)
     */
    @GetMapping("/me")
    @Operation(
            summary = "현재 커플 정보 조회",
            description = "사용자가 현재 속해있는 커플 정보를 조회합니다. 커플 상태가 아니면 404를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "커플 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "커플 상태가 아닙니다")
    })
    public ResponseEntity<CoupleResponse> getCurrentCouple(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return coupleService.getCurrentCouple(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
