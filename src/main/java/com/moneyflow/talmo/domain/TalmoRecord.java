package com.moneyflow.talmo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "talmo_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TalmoRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private TalmoUser user;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "time_display", length = 20)
    private String timeDisplay;

    @Column(name = "time_ms", nullable = false)
    private Integer timeMs;

    @Column(nullable = false)
    private Integer errors;

    @Column(name = "task_count")
    private Integer taskCount;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Builder
    public TalmoRecord(TalmoUser user, String topic, String timeDisplay,
                       Integer timeMs, Integer errors, Integer taskCount,
                       LocalDateTime completedAt) {
        this.user = user;
        this.topic = topic;
        this.timeDisplay = timeDisplay;
        this.timeMs = timeMs;
        this.errors = errors;
        this.taskCount = taskCount;
        this.completedAt = completedAt != null ? completedAt : LocalDateTime.now();
    }
}
