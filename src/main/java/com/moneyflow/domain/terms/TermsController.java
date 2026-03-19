package com.moneyflow.domain.terms;

import com.moneyflow.dto.request.AgreementDto;
import com.moneyflow.dto.response.TermsDocumentResponse;
import com.moneyflow.dto.response.UserAgreementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 약관 관리 컨트롤러
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Terms", description = "약관 관리 API")
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/terms/active")
    @Operation(summary = "현재 유효한 약관 목록 조회", description = "회원가입 또는 재동의 시 사용")
    public ResponseEntity<List<TermsDocumentResponse>> getActiveTerms() {
        return ResponseEntity.ok(termsService.getActiveTerms());
    }

    @GetMapping("/users/me/agreements")
    @Operation(summary = "내 약관 동의 이력 조회", description = "로그인한 사용자의 약관 동의 이력 조회")
    public ResponseEntity<List<UserAgreementResponse>> getMyAgreements(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        return ResponseEntity.ok(termsService.getUserAgreements(userId));
    }

    @PostMapping("/users/me/agreements")
    @Operation(summary = "약관 재동의", description = "약관 버전 변경 시 재동의")
    public ResponseEntity<Void> consentAgreements(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody List<AgreementDto> agreements,
        HttpServletRequest request
    ) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new com.moneyflow.exception.UnauthorizedException("인증 정보가 없습니다");
        }
        UUID userId = UUID.fromString(userDetails.getUsername());
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        termsService.saveAgreements(userId, agreements, ipAddress, userAgent);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/users/me/marketing-consent")
    @Operation(summary = "마케팅 수신 동의 변경", description = "설정 화면에서 마케팅 수신 동의/거부")
    public ResponseEntity<Void> updateMarketingConsent(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestBody Map<String, Boolean> request,
        HttpServletRequest httpRequest
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Boolean agreed = request.get("agreed");
        if (agreed == null) {
            throw new IllegalArgumentException("agreed 필드는 필수입니다.");
        }

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        termsService.updateMarketingConsent(userId, agreed, ipAddress, userAgent);

        return ResponseEntity.ok().build();
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For에 여러 IP가 있을 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
