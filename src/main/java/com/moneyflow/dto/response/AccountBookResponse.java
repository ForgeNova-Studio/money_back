package com.moneyflow.dto.response;

import com.moneyflow.domain.accountbook.BookType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 장부 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBookResponse {

    private UUID accountBookId;
    private String name;
    private BookType bookType;
    private UUID coupleId;
    private Integer memberCount;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private LocalDateTime createdAt;

    /**
     * 멤버 목록
     */
    private List<MemberInfo> members;

    /**
     * 멤버 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberInfo {
        private UUID userId;
        private String nickname;
        private String email;
        private String role;
        private LocalDateTime joinedAt;
    }
}
