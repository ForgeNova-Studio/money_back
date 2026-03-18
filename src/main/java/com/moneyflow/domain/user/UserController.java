package com.moneyflow.domain.user;

import com.moneyflow.dto.request.NicknameUpdateRequest;
import com.moneyflow.dto.request.WithdrawRequest;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.exception.UnauthorizedException;
import com.moneyflow.service.AuthService;
import com.moneyflow.service.UserProfileService;
import com.moneyflow.service.UserWithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 API")
public class UserController {

    private final AuthService authService;
    private final UserWithdrawalService userWithdrawalService;
    private final UserProfileService userProfileService;

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

    @PatchMapping("/me/nickname")
    @Operation(
            summary = "닉네임 수정",
            description = "현재 로그인한 사용자의 닉네임을 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "닉네임 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (빈 닉네임 또는 50자 초과)",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"code\": \"C002\", \"message\": \"입력값 검증에 실패했습니다\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"code\": \"A007\", \"message\": \"인증되지 않은 사용자입니다\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"code\": \"U001\", \"message\": \"사용자를 찾을 수 없습니다\"}")
                    )
            )
    })
    public ResponseEntity<UserInfoResponse> updateNickname(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        UUID userId = extractUserId(userDetails);
        UserInfoResponse response = userProfileService.updateNickname(userId, request.getNickname());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자의 계정을 영구 삭제합니다. " +
                    "이메일 회원은 비밀번호 확인이 필요하며, SNS 회원은 확인 없이 탈퇴됩니다. " +
                    "탈퇴 시 모든 개인 데이터가 영구 삭제되며 복구할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "비밀번호가 올바르지 않음 (이메일 회원)",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"error\": \"비밀번호가 올바르지 않습니다\"}")
                    )
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
    public ResponseEntity<Void> withdrawUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody WithdrawRequest request
    ) {
        UUID userId = extractUserId(userDetails);
        userWithdrawalService.withdraw(userId, request);
        return ResponseEntity.ok().build();
    }

    private UUID extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw UnauthorizedException.authentication("인증되지 않은 사용자입니다");
        }

        return UUID.fromString(userDetails.getUsername());
    }
}
