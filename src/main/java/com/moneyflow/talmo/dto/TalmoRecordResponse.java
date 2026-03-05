package com.moneyflow.talmo.dto;

import com.moneyflow.talmo.domain.TalmoRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TalmoRecordResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String topic;
    private String time;
    private Integer timeMs;
    private Integer errors;
    private Integer taskCount;
    private LocalDateTime completedAt;

    public static TalmoRecordResponse from(TalmoRecord record) {
        return TalmoRecordResponse.builder()
                .id(record.getId())
                .userId(record.getUser().getId())
                .userName(record.getUser().getName())
                .topic(record.getTopic())
                .time(record.getTimeDisplay())
                .timeMs(record.getTimeMs())
                .errors(record.getErrors())
                .taskCount(record.getTaskCount())
                .completedAt(record.getCompletedAt())
                .build();
    }
}
