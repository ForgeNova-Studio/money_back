package com.moneyflow.talmo.controller;

import com.moneyflow.talmo.config.KakaoConfig;
import com.moneyflow.talmo.domain.TalmoUser;
import com.moneyflow.talmo.repository.TalmoUserRepository;
import com.moneyflow.talmo.scheduler.TalmoScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 카카오 OAuth 로그인 → 토큰 저장
 *
 * 흐름:
 * 1. 프론트에서 /api/talmo/kakao/login?userId=3 → 카카오 로그인 페이지 리다이렉트
 * 2. 카카오 로그인 완료 → /api/talmo/kakao/callback 으로 code 전달
 * 3. code → access_token + refresh_token 교환
 * 4. talmo_users 테이블에 토큰 저장
 */
@RestController
@RequestMapping("/api/talmo/kakao")
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthController {

    private final KakaoConfig kakaoConfig;
    private final TalmoUserRepository userRepository;
    private final TalmoScheduler talmoScheduler;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 카카오 로그인 페이지로 리다이렉트
     * - userId를 state 파라미터로 전달하여 콜백 시 어떤 유저인지 식별
     */
    @GetMapping("/login")
    public ResponseEntity<Void> kakaoLogin(@RequestParam Long userId) {
        String url = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoConfig.getRestApiKey()
                + "&redirect_uri=" + kakaoConfig.getRedirectUri()
                + "&response_type=code"
                + "&scope=talk_message"
                + "&state=" + userId;

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    /**
     * 카카오 OAuth 콜백 — 인가 코드로 토큰 교환 후 DB 저장
     */
    @GetMapping("/callback")
    public ResponseEntity<String> kakaoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        Long userId;
        try {
            userId = Long.parseLong(state);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("유효하지 않은 state 파라미터");
        }

        TalmoUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("유저를 찾을 수 없습니다: " + userId);
        }

        // 인가 코드 → 토큰 교환
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", kakaoConfig.getRestApiKey());
            body.add("redirect_uri", kakaoConfig.getRedirectUri());
            body.add("code", code);

            if (kakaoConfig.getClientSecret() != null && !kakaoConfig.getClientSecret().isEmpty()) {
                body.add("client_secret", kakaoConfig.getClientSecret());
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    "https://kauth.kakao.com/oauth/token",
                    request,
                    Map.class);

            if (response == null) {
                return ResponseEntity.internalServerError().body("토큰 교환 실패");
            }

            String accessToken = (String) response.get("access_token");
            String refreshToken = (String) response.get("refresh_token");

            user.updateKakaoTokens(accessToken, refreshToken);
            userRepository.save(user);

            log.info("카카오 토큰 저장 완료 - userId: {}, userName: {}", userId, user.getName());

            // 성공 페이지 (간단한 HTML 반환)
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"><title>카카오 연결 완료</title></head>
                    <body style="background:#0a0e27;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif">
                    <div style="text-align:center">
                        <h1>✅ 카카오 알림 연결 완료!</h1>
                        <p>%s님, 이제 매일 코테 알림을 받을 수 있어요.</p>
                        <p>이 창을 닫아주세요.</p>
                    </div>
                    </body>
                    </html>
                    """
                    .formatted(user.getName());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (Exception e) {
            log.error("카카오 토큰 교환 실패", e);
            return ResponseEntity.internalServerError().body("카카오 토큰 교환 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오 연동 상태 확인
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getKakaoStatus(@PathVariable Long userId) {
        TalmoUser user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "kakaoLinked", user.hasKakaoToken()));
    }

    /**
     * 알림 발송 즉시 테스트 (개발/확인용)
     * 예: /api/talmo/kakao/test-notify?timeSlot=morning
     */
    @GetMapping("/test-notify")
    public ResponseEntity<String> testNotify(@RequestParam(defaultValue = "morning") String timeSlot) {
        try {
            talmoScheduler.checkAndNotify(timeSlot);
            return ResponseEntity.ok("✅ 알림 트리거 성공! (" + timeSlot + ") 카카오톡을 확인해주세요.");
        } catch (Exception e) {
            log.error("테스트 발송 실패", e);
            return ResponseEntity.internalServerError().body("발송 실패: " + e.getMessage());
        }
    }
}
