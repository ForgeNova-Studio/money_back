package com.moneyflow.service;

import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.response.UserInfoResponse;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("닉네임 수정: 앞뒤 공백을 제거하고 저장한다")
    void updateNickname_trimsNicknameBeforeSave() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("user@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserInfoResponse response = userProfileService.updateNickname(userId, "  새 닉네임  ");

        assertThat(user.getNickname()).isEqualTo("새 닉네임");
        assertThat(response.getNickname()).isEqualTo("새 닉네임");
    }

    @Test
    @DisplayName("닉네임 수정: 사용자가 없으면 예외가 발생한다")
    void updateNickname_throwsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.updateNickname(userId, "새닉네임"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("닉네임 수정: 공백만 입력하면 예외가 발생한다")
    void updateNickname_throwsWhenNicknameIsBlankAfterTrim() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("user@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userProfileService.updateNickname(userId, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("닉네임은 필수입니다");
    }

    @Test
    @DisplayName("닉네임 수정: 50자를 초과하면 예외가 발생한다")
    void updateNickname_throwsWhenNicknameIsTooLong() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("user@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userProfileService.updateNickname(userId, "a".repeat(51)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("50자 이하여야 합니다");
    }

    @Test
    @DisplayName("닉네임 수정: 허용되지 않은 문자가 포함되면 예외가 발생한다")
    void updateNickname_throwsWhenNicknameContainsDisallowedCharacters() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("user@test.com")
                .nickname("기존닉네임")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userProfileService.updateNickname(userId, "<script>alert('xss')</script>"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("한글, 영문, 숫자, 공백, ., _, - 만");
    }
}
