package com.moneyflow.talmo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TalmoRecordRequest {

    @NotNull(message = "userId는 필수입니다")
    private Long userId;

    @NotBlank(message = "topic은 필수입니다")
    private String topic;

    private String time; // 표시용 시간 (예: "01:23.45")

    @NotNull(message = "timeMs는 필수입니다")
    @Min(0)
    private Integer timeMs;

    @NotNull
    @Min(0)
    private Integer errors;

    private Integer taskCount;
}
