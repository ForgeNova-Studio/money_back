package com.moneyflow.domain.user;

import com.moneyflow.dto.request.SocialLoginRequest;
import com.moneyflow.dto.response.LoginResponse;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.service.AuthService;
import com.moneyflow.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Profile("dev")
@Tag(name = "Auth Dev", description = "개발 전용 인증 API")
public class AuthDevController {

    private final AuthService authService;
    private final SocialLoginService socialLoginService;

    @PostMapping("/social-login/mock")
    @Operation(summary = "[개발용] Mock 소셜 로그인", description = "⚠️ 개발/테스트 전용 엔드포인트입니다. " +
            "실제 ID Token/Access Token 없이 소셜 로그인을 테스트할 수 있습니다. " +
            "지원: GOOGLE, NAVER, KAKAO. " +
            "idToken 필드에 임의의 문자열을 입력하면 됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mock 소셜 로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    })
    public ResponseEntity<LoginResponse> mockSocialLogin(
            @Valid @RequestBody SocialLoginRequest request) {

        LoginResponse response = socialLoginService.mockLogin(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dev/users/all")
    @Operation(summary = "[개발용] 전체 유저 목록 조회", description = "⚠️ 개발/테스트 전용 엔드포인트입니다. " +
            "데이터베이스에 등록된 모든 유저의 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 목록 조회 성공", content = @Content(schema = @Schema(implementation = UserInfoResponse.class)))
    })
    public ResponseEntity<java.util.List<UserInfoResponse>> getAllUsers() {
        java.util.List<UserInfoResponse> users = authService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/dev/users")
    @Operation(summary = "[개발용] 이메일로 유저 조회", description = "⚠️ 개발/테스트 전용 엔드포인트입니다. " +
            "이메일로 유저 정보를 간단하게 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 조회 성공", content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "{\"error\": \"사용자를 찾을 수 없습니다\"}")))
    })
    public ResponseEntity<UserInfoResponse> getUserByEmail(
            @RequestParam("email") String email) {
        UserInfoResponse response = authService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/dev/users")
    @Operation(summary = "[개발용] 유저 완전 삭제", description = "⚠️ 개발/테스트 전용 엔드포인트입니다. " +
            "이메일로 유저를 데이터베이스에서 완전히 삭제합니다. 이 작업은 되돌릴 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 삭제 성공", content = @Content(examples = @ExampleObject(value = "{\"message\": \"유저가 삭제되었습니다\"}"))),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음", content = @Content(examples = @ExampleObject(value = "{\"error\": \"사용자를 찾을 수 없습니다\"}")))
    })
    public ResponseEntity<MessageResponse> deleteUserByEmail(
            @RequestParam("email") String email) {
        authService.deleteUserByEmail(email);
        return ResponseEntity.ok(new MessageResponse("유저가 삭제되었습니다"));
    }

    private record MessageResponse(String message) {
    }
}
