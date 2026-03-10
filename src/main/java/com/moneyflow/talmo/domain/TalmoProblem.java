package com.moneyflow.talmo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "talmo_problems")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TalmoProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private TalmoUser user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 50)
    private String source; // programmers, baekjoon, leetcode

    @Column(length = 20)
    private String difficulty; // Lv.1~5 or 브론즈~다이아

    @Column(name = "problem_url", length = 500)
    private String problemUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "io_example", columnDefinition = "TEXT")
    private String ioExample; // 탭 구분 원본 텍스트 저장

    @Column(name = "io_explanation", columnDefinition = "TEXT")
    private String ioExplanation; // 입출력 예 설명

    @Column(name = "solution_code", columnDefinition = "TEXT")
    private String solutionCode;

    @Column(name = "solution_note", columnDefinition = "TEXT")
    private String solutionNote;

    @Column(name = "time_complexity", length = 50)
    private String timeComplexity;

    @Column(name = "space_complexity", length = 50)
    private String spaceComplexity;

    @Column(name = "complexity_reason", columnDefinition = "TEXT")
    private String complexityReason;

    @Column(name = "complexity_confidence")
    private Integer complexityConfidence;

    @Column(length = 200)
    private String tags; // 쉼표로 구분: "문자열,DP,그리디"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "solution_version", nullable = false)
    private Integer solutionVersion;

    @Builder
    public TalmoProblem(TalmoUser user, String title, String source, String difficulty,
            String problemUrl, String description, String ioExample,
            String ioExplanation, String solutionCode, String solutionNote,
            String timeComplexity, String spaceComplexity, String complexityReason,
            Integer complexityConfidence, String tags, LocalDateTime createdAt,
            LocalDateTime updatedAt, Integer solutionVersion) {
        this.user = user;
        this.title = title;
        this.source = source;
        this.difficulty = difficulty;
        this.problemUrl = problemUrl;
        this.description = description;
        this.ioExample = ioExample;
        this.ioExplanation = ioExplanation;
        this.solutionCode = solutionCode;
        this.solutionNote = solutionNote;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.complexityReason = complexityReason;
        this.complexityConfidence = complexityConfidence;
        this.tags = tags;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.solutionVersion = solutionVersion != null ? solutionVersion : 1;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
        if (this.solutionVersion == null) {
            this.solutionVersion = 1;
        }
    }

    public void updateProblem(String title, String source, String difficulty,
            String problemUrl, String description, String ioExample,
            String ioExplanation, String solutionCode, String solutionNote,
            String timeComplexity, String spaceComplexity, String complexityReason,
            Integer complexityConfidence, String tags) {
        this.title = title;
        this.source = source;
        this.difficulty = difficulty;
        this.problemUrl = problemUrl;
        this.description = description;
        this.ioExample = ioExample;
        this.ioExplanation = ioExplanation;
        this.solutionCode = solutionCode;
        this.solutionNote = solutionNote;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.complexityReason = complexityReason;
        this.complexityConfidence = complexityConfidence;
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
        this.solutionVersion = (this.solutionVersion == null ? 1 : this.solutionVersion) + 1;
    }
}
