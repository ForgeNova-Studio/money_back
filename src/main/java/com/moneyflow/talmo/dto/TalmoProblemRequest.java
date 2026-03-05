package com.moneyflow.talmo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TalmoProblemRequest {

    @NotNull(message = "userId는 필수입니다")
    private Long userId;

    @NotBlank(message = "title은 필수입니다")
    private String title;

    private String source; // programmers, baekjoon, leetcode
    private String difficulty; // Lv.1~5
    private String problemUrl;
    private String description;
    private String ioExample; // 탭 구분 원본 텍스트
    private String ioExplanation; // 입출력 예 설명
    private String solutionCode;
    private String solutionNote;
    private String tags; // 쉼표로 구분
}
