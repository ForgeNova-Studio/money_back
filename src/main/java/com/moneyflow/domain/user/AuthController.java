package com.moneyflow.domain.user;

import com.moneyflow.dto.request.*;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.dto.response.RegisterResponse;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.dto.response.VerificationResponse;
import com.moneyflow.service.AuthService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;

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
    @Operation(
            summary = "사용자 정보 조회",
            description = "JWT 토큰을 이용해 현재 로그인한 사용자의 정보를 조회합니다. " +
                    "Authorization 헤더에 Bearer 토큰을 포함하여 요청해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자 (토큰 없음 또는 만료)",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"인증되지 않은 사용자입니다\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"사용자를 찾을 수 없습니다\"}")
                    )
            )
    })
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        UserInfoResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social-login")
    @Operation(
            summary = "소셜 로그인",
            description = "Google, Apple, Naver, Kakao 소셜 로그인을 처리합니다. " +
                    "클라이언트에서 받은 ID Token 또는 Access Token을 검증하고 JWT 토큰을 발급합니다. " +
                    "(Google/Apple: ID Token, Naver/Kakao: Access Token) " +
                    "신규 사용자의 경우 자동으로 회원가입이 진행됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "소셜 로그인 성공 (기존 사용자)",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "201",
                    description = "소셜 로그인 성공 (신규 가입)",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 ID Token 또는 이미 다른 방법으로 가입된 이메일",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = "{\"error\": \"유효하지 않은 소셜 로그인 토큰입니다\"}"
                            )
                    )
            )
    })
    public ResponseEntity<LoginResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request) {

        LoginResponse response = authService.socialLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/social-login/mock")
    @Operation(
            summary = "[개발용] Mock 소셜 로그인",
            description = "⚠️ 개발/테스트 전용 엔드포인트입니다. " +
                    "실제 ID Token/Access Token 없이 소셜 로그인을 테스트할 수 있습니다. " +
                    "지원: GOOGLE, APPLE, NAVER, KAKAO. " +
                    "idToken 필드에 임의의 문자열을 입력하면 됩니다. " +
                    "프로덕션 환경에서는 비활성화되어야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mock 소셜 로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            )
    })
    public ResponseEntity<LoginResponse> mockSocialLogin(
            @Valid @RequestBody SocialLoginRequest request) {

        LoginResponse response = authService.mockSocialLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-signup-code")
    @Operation(
            summary = "회원가입 인증 코드 발송",
            description = "회원가입을 위한 6자리 인증 코드를 이메일로 발송합니다. 인증 코드는 10분간 유효합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 코드 발송 성공",
                    content = @Content(schema = @Schema(implementation = VerificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 가입된 이메일",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"이미 가입된 이메일입니다\"}")
                    )
            )
    })
    public ResponseEntity<VerificationResponse> sendSignupCode(
            @Valid @RequestBody SendCodeRequest request) {
        VerificationResponse response = authService.sendSignupCode(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-signup-code")
    @Operation(
            summary = "회원가입 인증 코드 검증",
            description = "발송된 6자리 인증 코드를 검증합니다. 검증 성공 후 회원가입을 진행할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 성공",
                    content = @Content(schema = @Schema(implementation = VerificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "인증 실패 (코드 불일치, 만료 등)",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"인증 코드가 일치하지 않습니다\"}")
                    )
            )
    })
    public ResponseEntity<VerificationResponse> verifySignupCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        VerificationResponse response = authService.verifySignupCode(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password/send-code")
    @Operation(
            summary = "비밀번호 재설정 인증 코드 발송",
            description = "비밀번호 재설정을 위한 6자리 인증 코드를 이메일로 발송합니다. 인증 코드는 10분간 유효합니다. 소셜 로그인 사용자는 사용할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 코드 발송 성공",
                    content = @Content(schema = @Schema(implementation = VerificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "가입되지 않은 이메일 또는 소셜 로그인 사용자",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다\"}")
                    )
            )
    })
    public ResponseEntity<VerificationResponse> sendPasswordResetCode(
            @Valid @RequestBody SendCodeRequest request) {
        VerificationResponse response = authService.sendPasswordResetCode(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "비밀번호 재설정",
            description = "인증 코드를 검증하고 새로운 비밀번호로 재설정합니다. 소셜 로그인 사용자는 사용할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 재설정 성공",
                    content = @Content(schema = @Schema(implementation = VerificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "인증 실패 또는 소셜 로그인 사용자",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"인증 코드가 일치하지 않습니다\"}")
                    )
            )
    })
    public ResponseEntity<VerificationResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        VerificationResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "JWT 토큰 갱신",
            description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다. " +
                    "Access Token이 만료되었을 때 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않거나 만료된 Refresh Token",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"유효하지 않거나 만료된 Refresh Token입니다\"}")
                    )
            )
    })
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody com.moneyflow.dto.request.RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "API 서버 상태 확인")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }

    // ========== 개발용 API ==========

    @GetMapping("/dev/users/all")
    @Operation(
            summary = "[개발용] 전체 유저 목록 조회",
            description = "️ 개발/테스트 전용 엔드포인트입니다. " +
                    "데이터베이스에 등록된 모든 유저의 목록을 조회합니다. " +
                    "프로덕션 환경에서는 비활성화되어야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            )
    })
    public ResponseEntity<java.util.List<UserInfoResponse>> getAllUsers() {
        java.util.List<UserInfoResponse> users = authService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/dev/users")
    @Operation(
            summary = "[개발용] 이메일로 유저 조회",
            description = "️ 개발/테스트 전용 엔드포인트입니다. " +
                    "이메일로 유저 정보를 간단하게 조회합니다. " +
                    "프로덕션 환경에서는 비활성화되어야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "유저를 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"사용자를 찾을 수 없습니다\"}")
                    )
            )
    })
    public ResponseEntity<UserInfoResponse> getUserByEmail(
            @RequestParam("email") String email) {
        UserInfoResponse response = authService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/dev/users")
    @Operation(
            summary = "[개발용] 유저 완전 삭제",
            description = "️ 개발/테스트 전용 엔드포인트입니다. " +
                    "이메일로 유저를 데이터베이스에서 완전히 삭제합니다. " +
                    "이 작업은 되돌릴 수 없습니다. " +
                    "프로덕션 환경에서는 비활성화되어야 합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "유저 삭제 성공",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"message\": \"유저가 삭제되었습니다\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "유저를 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"사용자를 찾을 수 없습니다\"}")
                    )
            )
    })
    public ResponseEntity<?> deleteUserByEmail(
            @RequestParam("email") String email) {
        authService.deleteUserByEmail(email);
        return ResponseEntity.ok().body(new MessageResponse("유저가 삭제되었습니다"));
    }

    // 간단한 응답용 DTO
    private record MessageResponse(String message) {}
}
