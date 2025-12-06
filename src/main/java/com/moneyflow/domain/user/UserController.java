package com.moneyflow.domain.user;

import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 API")
public class UserController {

    private final AuthService authService;

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
}
