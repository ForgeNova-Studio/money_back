package com.moneyflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 초대 코드 생성 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteResponse {

    /**
     * 생성된 초대 코드
     */
    private String inviteCode;

    /**
     * 초대 코드 만료 시간
     */
    private LocalDateTime expiresAt;

    /**
     * 메시지
     */
    private String message;
}
