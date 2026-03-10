package com.moneyflow.talmo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TalmoProblemAnalysisPromptResponse {
    private Long problemId;
    private Integer solutionVersion;
    private Long userId;
    private String userName;
    private String title;
    private String prompt;
}
