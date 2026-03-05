package com.moneyflow.talmo.dto;

import com.moneyflow.talmo.domain.TalmoUser;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TalmoUserResponse {
    private Long id;
    private String name;

    public static TalmoUserResponse from(TalmoUser user) {
        return TalmoUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
