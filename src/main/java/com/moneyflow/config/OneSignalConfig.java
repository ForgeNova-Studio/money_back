package com.moneyflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "onesignal")
@Getter
@Setter
public class OneSignalConfig {
    private String appId;
    private String apiKey;
}
