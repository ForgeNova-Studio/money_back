package com.moneyflow.talmo.config;

import com.moneyflow.talmo.domain.TalmoUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TalmoAdminPolicy {

    private final Set<String> adminNames;

    public TalmoAdminPolicy(@Value("${talmo.admin-names:}") String adminNames) {
        this.adminNames = Arrays.stream(adminNames.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    public boolean isAdmin(TalmoUser user) {
        if (user == null) {
            return false;
        }
        if (adminNames.isEmpty()) {
            return true;
        }
        return adminNames.contains(user.getName());
    }
}
