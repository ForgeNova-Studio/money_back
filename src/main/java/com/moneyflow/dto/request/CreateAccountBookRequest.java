package com.moneyflow.dto.request;

import com.moneyflow.domain.accountbook.BookType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 장부 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountBookRequest {

    /**
     * 장부 이름 (예: "2025 생활비", "일본 여행")
     */
    @NotBlank(message = "장부 이름은 필수입니다")
    @Size(max = 100, message = "장부 이름은 100자 이하여야 합니다")
    private String name;

    /**
     * 장부 유형
     */
    @NotNull(message = "장부 유형은 필수입니다")
    private BookType bookType;

    /**
     * 커플 ID (커플 장부인 경우)
     */
    private UUID coupleId;

    /**
     * 정산용 인원수
     */
    private Integer memberCount;

    /**
     * 장부 설명
     */
    private String description;

    /**
     * 시작일
     */
    private LocalDate startDate;

    /**
     * 종료일
     */
    private LocalDate endDate;
}
