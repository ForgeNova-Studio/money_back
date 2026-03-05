package com.moneyflow.talmo.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TalmoTodayResponse {
    private List<String> completedUsers;
    private List<String> pendingUsers;
}
