package com.moneyflow.service;

import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DevelopmentOnlyAccessGuard {

    private final Environment environment;

    public void validate(String featureName) {
        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            throw UnauthorizedException.accessDenied(featureName + " 기능은 dev 프로파일에서만 사용할 수 있습니다");
        }
    }
}
