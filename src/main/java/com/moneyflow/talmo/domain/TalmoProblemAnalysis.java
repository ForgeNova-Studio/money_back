package com.moneyflow.talmo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "talmo_problem_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TalmoProblemAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private TalmoProblem problem;

    @Column(name = "solution_version", nullable = false)
    private Integer solutionVersion;

    @Column(name = "time_complexity", length = 50)
    private String timeComplexity;

    @Column(name = "space_complexity", length = 50)
    private String spaceComplexity;

    @Column(name = "approach_summary", columnDefinition = "TEXT")
    private String approachSummary;

    @Column(name = "analysis_text", columnDefinition = "TEXT")
    private String analysisText;

    @Column(name = "improvement_possible", nullable = false)
    private boolean improvementPossible;

    @Column(name = "better_approach", columnDefinition = "TEXT")
    private String betterApproach;

    @Column(name = "better_time_complexity", length = 50)
    private String betterTimeComplexity;

    @Column(name = "better_space_complexity", length = 50)
    private String betterSpaceComplexity;

    @Column(name = "prompt_snapshot", columnDefinition = "TEXT")
    private String promptSnapshot;

    @Column(name = "ai_raw_response", columnDefinition = "TEXT")
    private String aiRawResponse;

    @Column(name = "analysis_status", nullable = false, length = 30)
    private String analysisStatus;

    @Column(name = "notification_status", nullable = false, length = 30)
    private String notificationStatus;

    @Column(name = "analyzed_by_name", length = 50)
    private String analyzedByName;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TalmoProblemAnalysis(TalmoProblem problem, Integer solutionVersion, String timeComplexity,
            String spaceComplexity, String approachSummary, String analysisText,
            boolean improvementPossible, String betterApproach, String betterTimeComplexity,
            String betterSpaceComplexity, String promptSnapshot, String aiRawResponse,
            String analysisStatus, String notificationStatus, String analyzedByName,
            LocalDateTime analyzedAt, LocalDateTime notifiedAt, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.problem = problem;
        this.solutionVersion = solutionVersion;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.approachSummary = approachSummary;
        this.analysisText = analysisText;
        this.improvementPossible = improvementPossible;
        this.betterApproach = betterApproach;
        this.betterTimeComplexity = betterTimeComplexity;
        this.betterSpaceComplexity = betterSpaceComplexity;
        this.promptSnapshot = promptSnapshot;
        this.aiRawResponse = aiRawResponse;
        this.analysisStatus = analysisStatus;
        this.notificationStatus = notificationStatus;
        this.analyzedByName = analyzedByName;
        this.analyzedAt = analyzedAt;
        this.notifiedAt = notifiedAt;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
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
        if (this.analysisStatus == null || this.analysisStatus.isBlank()) {
            this.analysisStatus = "SAVED";
        }
        if (this.notificationStatus == null || this.notificationStatus.isBlank()) {
            this.notificationStatus = "NOT_REQUIRED";
        }
    }

    public void markNotificationStatus(String notificationStatus, LocalDateTime notifiedAt) {
        this.notificationStatus = notificationStatus;
        this.notifiedAt = notifiedAt;
        this.updatedAt = LocalDateTime.now();
    }
}
