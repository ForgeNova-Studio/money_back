package com.moneyflow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 테스트용 컨트롤러 (개발 환경 전용)
 * 프로덕션 배포 전 삭제 필요
 */
@RestController
@Profile("dev")
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final PasswordEncoder passwordEncoder;

    /**
     * BCrypt 해시 생성 테스트
     * 예: GET /api/test/hash?password=password123
     */
    @GetMapping("/hash")
    public Map<String, String> generateHash(@RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        Map<String, String> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        return result;
    }

    /**
     * BCrypt 해시 검증 테스트
     * 예: GET /api/test/verify?password=password123&hash=$2a$10$...
     */
    @GetMapping("/verify")
    public Map<String, Object> verifyHash(
            @RequestParam String password,
            @RequestParam String hash) {
        boolean matches = passwordEncoder.matches(password, hash);
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", matches);
        return result;
    }

    /**
     * Sentry 에러 테스트
     * 예: GET /api/test/sentry
     * 호출하면 Sentry 대시보드에서 에러 확인 가능
     */
    @GetMapping("/sentry")
    public Map<String, String> testSentry() {
        try {
            throw new RuntimeException("Sentry 테스트 에러 - 정상 작동 중!");
        } catch (Exception e) {
            io.sentry.Sentry.captureException(e);
            Map<String, String> result = new HashMap<>();
            result.put("status", "error_sent");
            result.put("message", "Sentry 대시보드에서 에러를 확인하세요!");
            return result;
        }
    }
}
