package com.moneyflow.talmo.dto;

import com.moneyflow.talmo.domain.TalmoProblem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TalmoProblemResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String title;
    private String source;
    private String difficulty;
    private String problemUrl;
    private String description;
    private String ioExample;
    private String ioExplanation;
    private String solutionCode;
    private String solutionNote;
    private String timeComplexity;
    private String spaceComplexity;
    private String complexityReason;
    private Integer complexityConfidence;
    private String tags;
    private LocalDateTime createdAt;

    public static TalmoProblemResponse from(TalmoProblem problem) {
        return TalmoProblemResponse.builder()
                .id(problem.getId())
                .userId(problem.getUser().getId())
                .userName(problem.getUser().getName())
                .title(problem.getTitle())
                .source(problem.getSource())
                .difficulty(problem.getDifficulty())
                .problemUrl(problem.getProblemUrl())
                .description(problem.getDescription())
                .ioExample(problem.getIoExample())
                .ioExplanation(problem.getIoExplanation())
                .solutionCode(problem.getSolutionCode())
                .solutionNote(problem.getSolutionNote())
                .timeComplexity(problem.getTimeComplexity())
                .spaceComplexity(problem.getSpaceComplexity())
                .complexityReason(problem.getComplexityReason())
                .complexityConfidence(problem.getComplexityConfidence())
                .tags(problem.getTags())
                .createdAt(problem.getCreatedAt())
                .build();
    }
}
