package com.moneyflow.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 장부 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccountBookRequest {

    /**
     * 장부 이름
     */
    @Size(max = 100, message = "장부 이름은 100자 이하여야 합니다")
    private String name;

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
