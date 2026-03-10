package com.moneyflow.talmo.dto;

import com.moneyflow.talmo.domain.TalmoProblemAnalysis;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TalmoProblemAnalysisResponse {
    private Long id;
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
    private String analysisStatus;
    private String notificationStatus;
    private String analyzedByName;
    private LocalDateTime analyzedAt;
    private LocalDateTime notifiedAt;

    public static TalmoProblemAnalysisResponse from(TalmoProblemAnalysis analysis) {
        if (analysis == null) {
            return null;
        }

        return TalmoProblemAnalysisResponse.builder()
                .id(analysis.getId())
                .solutionVersion(analysis.getSolutionVersion())
                .timeComplexity(analysis.getTimeComplexity())
                .spaceComplexity(analysis.getSpaceComplexity())
                .approachSummary(analysis.getApproachSummary())
                .analysisText(analysis.getAnalysisText())
                .improvementPossible(analysis.isImprovementPossible())
                .betterApproach(analysis.getBetterApproach())
                .betterTimeComplexity(analysis.getBetterTimeComplexity())
                .betterSpaceComplexity(analysis.getBetterSpaceComplexity())
                .promptSnapshot(analysis.getPromptSnapshot())
                .aiRawResponse(analysis.getAiRawResponse())
                .analysisStatus(analysis.getAnalysisStatus())
                .notificationStatus(analysis.getNotificationStatus())
                .analyzedByName(analysis.getAnalyzedByName())
                .analyzedAt(analysis.getAnalyzedAt())
                .notifiedAt(analysis.getNotifiedAt())
                .build();
    }
}
