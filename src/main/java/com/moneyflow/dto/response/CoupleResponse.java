package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 커플 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoupleResponse {

    /**
     * 커플 ID
     */
    private UUID coupleId;

    /**
     * User 1 정보
     */
    private UserInfo user1;

    /**
     * User 2 정보 (연동 전에는 null)
     */
    private UserInfo user2;

    /**
     * 초대 코드 (연동 전에만 표시)
     */
    private String inviteCode;

    /**
     * 초대 코드 만료 시간
     */
    private LocalDateTime codeExpiresAt;

    /**
     * 커플 연동 완료 여부
     */
    private boolean linked;

    /**
     * 연동 완료 시간
     */
    private LocalDateTime linkedAt;

    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 사용자 정보 (간략)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID userId;
        private String nickname;
        private String email;
    }
}
