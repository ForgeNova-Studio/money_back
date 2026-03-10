package com.moneyflow.talmo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TalmoProblemAnalysisRequest {

    @NotNull(message = "adminUserId는 필수입니다")
    private Long adminUserId;

    private Integer solutionVersion;
    private String timeComplexity;
    private String spaceComplexity;
    private String approachSummary;
    private String analysisText;
    private Boolean improvementPossible;
    private String betterApproach;
    private String betterTimeComplexity;
    private String betterSpaceComplexity;
    private String promptSnapshot;
    private String aiRawResponse;
}
