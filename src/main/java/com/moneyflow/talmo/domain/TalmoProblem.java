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

    @Column(length = 200)
    private String tags; // 쉼표로 구분: "문자열,DP,그리디"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public TalmoProblem(TalmoUser user, String title, String source, String difficulty,
            String problemUrl, String description, String ioExample,
            String ioExplanation, String solutionCode, String solutionNote, String tags,
            LocalDateTime createdAt) {
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
        this.tags = tags;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
