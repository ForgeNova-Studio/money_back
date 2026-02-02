package com.moneyflow.domain.notification;

import com.moneyflow.config.OneSignalConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OneSignalService {

    private final OneSignalConfig oneSignalConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications";

    /**
     * 특정 사용자들에게 알림 발송
     * 
     * @param targetExternalUserIds OneSignal에 등록된 External User IDs (여기서는 User
     *                              UUID)
     */
    @Async
    public void sendNotification(String title, String message, List<String> targetExternalUserIds) {
        if (targetExternalUserIds == null || targetExternalUserIds.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("app_id", oneSignalConfig.getAppId());
            body.put("contents", Map.of("en", message)); // 영어/한글 구분 없이 내용을 contents에 넣음
            body.put("headings", Map.of("en", title));
            body.put("include_external_user_ids", targetExternalUserIds);

            // Channel ID (Android 8.0+) - 중요도 높음 등 설정 가능
            // body.put("android_channel_id", "...");

            log.info("Sending OneSignal notification to users: {}", targetExternalUserIds);
            sendRequest(body);
        } catch (Exception e) {
            log.error("Failed to send OneSignal notification", e);
        }
    }

    /**
     * 모든 사용자에게 알림 발송 (Segments: All)
     */
    @Async
    public void sendNotificationToAll(String title, String message) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("app_id", oneSignalConfig.getAppId());
            body.put("contents", Map.of("en", message));
            body.put("headings", Map.of("en", title));
            body.put("included_segments", List.of("AllUsers")); // OneSignal 기본 세그먼트 이름 확인 필요 (보통 'All' 또는 'Subscribed
                                                                // Users')
                                                                // OneSignal V5부터는 'All' 지원 안 할 수도 있음 -> 'Subscribed
                                                                // Users' 확인
                                                                // 문서상 'included_segments': ["All"] 사용 가능.

            // 안전하게 'Subscribed Users' 세그먼트 사용 권장하지만, 기본은 'All' 시도
            body.put("included_segments", List.of("All"));

            log.info("Sending OneSignal notification to ALL users");
            sendRequest(body);
        } catch (Exception e) {
            log.error("Failed to send OneSignal notification to all", e);
        }
    }

    private void sendRequest(Map<String, Object> body) {
        if (oneSignalConfig.getApiKey() == null || oneSignalConfig.getApiKey().isBlank()) {
            log.warn("OneSignal API Key is missing. Skipping notification dispatch.");
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + oneSignalConfig.getApiKey());
        headers.set("accept", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(ONESIGNAL_API_URL, entity, String.class);
            log.info("OneSignal Response: {}", response);
        } catch (Exception e) {
            log.error("Error communicating with OneSignal API", e);
        }
    }
}
