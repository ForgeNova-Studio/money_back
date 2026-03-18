package com.moneyflow.domain.user;

import com.moneyflow.dto.request.*;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.dto.response.RegisterResponse;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.dto.response.VerificationResponse;
import com.moneyflow.service.AuthService;
import com.moneyflow.service.EmailVerificationService;
import com.moneyflow.service.SocialLoginService;
import com.moneyflow.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

        private final AuthService authService;
        private final EmailVerificationService emailVerificationService;
        private final SocialLoginService socialLoginService;
        private final TokenService tokenService;

        @PostMapping("/register")
        @Operation(summary = "회원가입", description = "이메일과 비밀번호로 새로운 계정을 생성합니다")
        public ResponseEntity<RegisterResponse> register(
                        @Valid @RequestBody RegisterRequest request) {

                RegisterResponse response = authService.register(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PostMapping("/login")
        @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다")
        public ResponseEntity<LoginResponse> login(
                        @Valid @RequestBody LoginRequest request) {

                LoginResponse response = authService.login(request);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/me")
        @Operation(summary = "사용자 정보 조회", description = "JWT 토큰을 이용해 현재 로그인한 사용자의 정보를 조회합니다. " +
                        "Authorization 헤더에 Bearer 토큰을 포함하여 요청해야 합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공", content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
                        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자 (토큰 없음 또는 만료)", content = @Content(examples = @ExampleObject(value = "{\"error\": \"인증되지 않은 사용자입니다\"}"))),
                        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "{\"error\": \"사용자를 찾을 수 없습니다\"}")))
        })
        public ResponseEntity<UserInfoResponse> getCurrentUser() {
                UserInfoResponse response = authService.getCurrentUser();
                return ResponseEntity.ok(response);
        }

        @PostMapping("/social-login")
        @Operation(summary = "소셜 로그인", description = "Google, Naver, Kakao 소셜 로그인을 처리합니다. " +
                        "클라이언트에서 받은 토큰(ID Token 또는 Access Token)을 검증하고 JWT 토큰을 발급합니다. " +
                        "(Google: ID Token, Naver/Kakao: Access Token) " +
                        "신규 사용자의 경우 자동으로 회원가입이 진행됩니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "소셜 로그인 성공 (기존 사용자)", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "201", description = "소셜 로그인 성공 (신규 가입)", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "400", description = "유효하지 않은 ID Token 또는 이미 다른 방법으로 가입된 이메일", content = @Content(examples = @ExampleObject(value = "{\"error\": \"유효하지 않은 소셜 로그인 토큰입니다\"}")))
        })
        public ResponseEntity<LoginResponse> socialLogin(
                        @Valid @RequestBody SocialLoginRequest request) {

                LoginResponse response = socialLoginService.login(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/send-signup-code")
        @Operation(summary = "회원가입 인증 코드 발송", description = "회원가입을 위한 6자리 인증 코드를 이메일로 발송합니다. 인증 코드는 10분간 유효합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공", content = @Content(schema = @Schema(implementation = VerificationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "이미 가입된 이메일", content = @Content(examples = @ExampleObject(value = "{\"error\": \"이미 가입된 이메일입니다\"}")))
        })
        public ResponseEntity<VerificationResponse> sendSignupCode(
                        @Valid @RequestBody SendCodeRequest request) {
                VerificationResponse response = emailVerificationService.sendSignupCode(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/verify-signup-code")
        @Operation(summary = "회원가입 인증 코드 검증", description = "발송된 6자리 인증 코드를 검증합니다. 검증 성공 후 5분 이내에 회원가입을 진행할 수 있습니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "인증 성공", content = @Content(schema = @Schema(implementation = VerificationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "인증 실패 (코드 불일치, 만료 등)", content = @Content(examples = @ExampleObject(value = "{\"error\": \"인증 코드가 일치하지 않습니다\"}")))
        })
        public ResponseEntity<VerificationResponse> verifySignupCode(
                        @Valid @RequestBody VerifyCodeRequest request) {
                VerificationResponse response = emailVerificationService.verifySignupCode(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/reset-password/send-code")
        @Operation(summary = "비밀번호 재설정 인증 코드 발송", description = "비밀번호 재설정을 위한 6자리 인증 코드를 이메일로 발송합니다. 인증 코드는 10분간 유효합니다. 소셜 로그인 사용자는 사용할 수 없습니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공", content = @Content(schema = @Schema(implementation = VerificationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "가입되지 않은 이메일 또는 소셜 로그인 사용자", content = @Content(examples = @ExampleObject(value = "{\"error\": \"소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다\"}")))
        })
        public ResponseEntity<VerificationResponse> sendPasswordResetCode(
                        @Valid @RequestBody SendCodeRequest request) {
                VerificationResponse response = emailVerificationService.sendPasswordResetCode(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/reset-password/verify-code")
        @Operation(summary = "비밀번호 재설정 인증 코드 검증", description = "발송된 6자리 인증 코드를 검증합니다. 검증 성공 후 5분 이내에 비밀번호를 재설정해야 합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "인증 성공", content = @Content(schema = @Schema(implementation = VerificationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "인증 실패 (코드 불일치, 만료 등)", content = @Content(examples = @ExampleObject(value = "{\"error\": \"인증 코드가 일치하지 않습니다\"}")))
        })
        public ResponseEntity<VerificationResponse> verifyPasswordResetCode(
                        @Valid @RequestBody VerifyCodeRequest request) {
                VerificationResponse response = emailVerificationService.verifyPasswordResetCode(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/reset-password")
        @Operation(summary = "비밀번호 재설정", description = "인증 완료 후 새로운 비밀번호로 재설정합니다. 인증 완료 후 5분 이내에 재설정해야 합니다. 소셜 로그인 사용자는 사용할 수 없습니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공", content = @Content(schema = @Schema(implementation = VerificationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "인증 미완료, 시간 만료, 또는 소셜 로그인 사용자", content = @Content(examples = @ExampleObject(value = "{\"error\": \"인증을 먼저 완료해주세요\"}")))
        })
        public ResponseEntity<VerificationResponse> resetPassword(
                        @Valid @RequestBody ChangePasswordRequest request) {
                VerificationResponse response = emailVerificationService.resetPassword(request);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/refresh")
        @Operation(summary = "JWT 토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다. "
                        +
                        "Access Token이 만료되었을 때 사용합니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 Refresh Token", content = @Content(examples = @ExampleObject(value = "{\"error\": \"유효하지 않거나 만료된 Refresh Token입니다\"}")))
        })
        public ResponseEntity<LoginResponse> refreshToken(
                        @Valid @RequestBody com.moneyflow.dto.request.RefreshTokenRequest request) {
                LoginResponse response = tokenService.rotate(request.getRefreshToken());
                return ResponseEntity.ok(response);
        }

        @PostMapping("/logout")
        @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하여 로그아웃합니다. " +
                        "이후 해당 Refresh Token으로는 새로운 Access Token을 발급받을 수 없습니다.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                        @ApiResponse(responseCode = "400", description = "유효하지 않은 Refresh Token", content = @Content(examples = @ExampleObject(value = "{\"error\": \"유효하지 않은 Refresh Token입니다\"}")))
        })
        public ResponseEntity<Void> logout(
                        @Valid @RequestBody com.moneyflow.dto.request.RefreshTokenRequest request) {
                tokenService.revoke(request.getRefreshToken());
                return ResponseEntity.ok().build();
        }

        @GetMapping("/health")
        @Operation(summary = "헬스 체크", description = "API 서버 상태 확인")
        public ResponseEntity<String> healthCheck() {
                return ResponseEntity.ok("OK");
        }
}
