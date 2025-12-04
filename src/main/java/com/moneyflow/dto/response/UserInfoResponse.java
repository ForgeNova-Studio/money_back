package com.moneyflow.dto.response;

import com.moneyflow.domain.user.Gender;
import com.moneyflow.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 사용자 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 정보 응답")
public class UserInfoResponse {

    @Schema(description = "사용자 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "프로필 이미지 URL (없으면 null)", example = "https://example.com/profile.jpg", nullable = true)
    private String profileImageUrl;

    @Schema(description = "성별 (MALE: 남성, FEMALE: 여성)", example = "MALE")
    private Gender gender;

    /**
     * User 엔티티로부터 UserInfoResponse 생성
     */
    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .build();
    }
}
