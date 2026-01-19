package com.moneyflow.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 테스트
 *
 * 모든 예외 유형에 대해 JSON 응답이 반환되는지 검증
 * Standalone MockMvc를 사용하여 순수 핸들러 테스트
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("AccessDeniedException 발생 시 JSON 403 응답")
    void handleAccessDeniedException_ReturnsJson403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("A003"))
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("AuthenticationException 발생 시 JSON 401 응답")
    void handleAuthenticationException_ReturnsJson401() throws Exception {
        mockMvc.perform(get("/test/auth-error"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("A007"))
                .andExpect(jsonPath("$.message").value("인증에 실패했습니다"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("BadCredentialsException 발생 시 JSON 401 응답")
    void handleBadCredentialsException_ReturnsJson401() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("A001"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("BusinessException 발생 시 JSON 응답 (ErrorCode 기반)")
    void handleBusinessException_ReturnsJsonWithErrorCode() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("U001"))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("IllegalArgumentException 발생 시 JSON 400 응답")
    void handleIllegalArgumentException_ReturnsJson400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 인자입니다"));
    }

    @Test
    @DisplayName("RuntimeException 발생 시 JSON 500 응답")
    void handleRuntimeException_ReturnsJson500() throws Exception {
        mockMvc.perform(get("/test/runtime-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("C003"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다"));
    }

    @Test
    @DisplayName("IOException (Checked Exception) 발생 시 JSON 500 응답")
    void handleCheckedException_ReturnsJson500() throws Exception {
        mockMvc.perform(get("/test/io-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("C003"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다"));
    }

    @Test
    @DisplayName("모든 에러 응답에 timestamp 포함")
    void allErrorResponses_IncludeTimestamp() throws Exception {
        mockMvc.perform(get("/test/runtime-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ===== 테스트용 컨트롤러 =====

    @RestController
    static class TestController {

        @GetMapping("/test/access-denied")
        public void accessDenied() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/test/auth-error")
        public void authError() {
            throw new TestAuthenticationException("Auth failed");
        }

        @GetMapping("/test/bad-credentials")
        public void badCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/test/business-error")
        public void businessError() {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        @GetMapping("/test/illegal-argument")
        public void illegalArgument() {
            throw new IllegalArgumentException("잘못된 인자입니다");
        }

        @GetMapping("/test/runtime-error")
        public void runtimeError() {
            throw new RuntimeException("Unexpected error");
        }

        @GetMapping("/test/io-error")
        public void ioError() throws IOException {
            throw new IOException("Disk full");
        }
    }

    // 테스트용 AuthenticationException 구현 (추상 클래스이므로 구현 필요)
    static class TestAuthenticationException extends AuthenticationException {
        public TestAuthenticationException(String msg) {
            super(msg);
        }
    }
}
