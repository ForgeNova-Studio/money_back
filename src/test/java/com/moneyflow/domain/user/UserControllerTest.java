package com.moneyflow.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyflow.dto.request.NicknameUpdateRequest;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import com.moneyflow.exception.GlobalExceptionHandler;
import com.moneyflow.service.AuthService;
import com.moneyflow.service.UserProfileService;
import com.moneyflow.service.UserWithdrawalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserWithdrawalService userWithdrawalService;

    @Mock
    private UserProfileService userProfileService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(authService, userWithdrawalService, userProfileService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("닉네임 수정 API: 정상 요청이면 수정된 사용자 정보를 반환한다")
    void updateNickname_returnsUpdatedUserInfo() throws Exception {
        UUID userId = UUID.randomUUID();
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(userId)
                .email("user@test.com")
                .nickname("새닉네임")
                .build();

        when(userProfileService.updateNickname(eq(userId), eq("새닉네임")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/users/me/nickname")
                        .with(authenticatedUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                NicknameUpdateRequest.builder().nickname("새닉네임").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("닉네임 수정 API: 빈 닉네임이면 400 응답을 반환한다")
    void updateNickname_returnsBadRequestWhenNicknameIsBlank() throws Exception {
        mockMvc.perform(patch("/api/users/me/nickname")
                        .with(authenticatedUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                NicknameUpdateRequest.builder().nickname("   ").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C002"))
                .andExpect(jsonPath("$.errors.nickname").value("닉네임은 필수입니다"));
    }

    @Test
    @DisplayName("닉네임 수정 API: 50자를 초과하면 400 응답을 반환한다")
    void updateNickname_returnsBadRequestWhenNicknameIsTooLong() throws Exception {
        UUID userId = UUID.randomUUID();
        String tooLongNickname = "a".repeat(51);

        when(userProfileService.updateNickname(eq(userId), eq(tooLongNickname)))
                .thenThrow(new BusinessException("닉네임은 50자 이하여야 합니다", ErrorCode.INVALID_NICKNAME));

        mockMvc.perform(patch("/api/users/me/nickname")
                        .with(authenticatedUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                NicknameUpdateRequest.builder().nickname(tooLongNickname).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U003"))
                .andExpect(jsonPath("$.message").value("닉네임은 50자 이하여야 합니다"));
    }

    @Test
    @DisplayName("닉네임 수정 API: 인증 정보가 없으면 401 응답을 반환한다")
    void updateNickname_returnsUnauthorizedWhenPrincipalMissing() throws Exception {
        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                NicknameUpdateRequest.builder().nickname("새닉네임").build())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A007"))
                .andExpect(jsonPath("$.message").value("인증되지 않은 사용자입니다"));
    }

    private RequestPostProcessor authenticatedUser(UUID userId) {
        return request -> {
            UserDetails principal = User.withUsername(userId.toString())
                    .password("")
                    .roles("USER")
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return request;
        };
    }
}
