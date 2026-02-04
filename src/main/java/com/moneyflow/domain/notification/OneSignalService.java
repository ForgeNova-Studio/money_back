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
        sendNotification(title, message, targetExternalUserIds, "NOTICE", null);
    }

    /**
     * 특정 사용자들에게 알림 발송 (타입 및 알림 ID 포함)
     */
    @Async
    public void sendNotification(String title, String message, List<String> targetExternalUserIds,
            String type, String notificationId) {
        if (targetExternalUserIds == null || targetExternalUserIds.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("app_id", oneSignalConfig.getAppId());
            body.put("contents", Map.of("en", message)); // 영어/한글 구분 없이 내용을 contents에 넣음
            body.put("headings", Map.of("en", title));
            body.put("include_external_user_ids", targetExternalUserIds);

            // 앱에서 분기 처리를 위한 추가 데이터
            Map<String, Object> data = new HashMap<>();
            data.put("type", type != null ? type : "NOTICE");
            if (notificationId != null) {
                data.put("notificationId", notificationId);
            }
            body.put("data", data);

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
            body.put("included_segments", List.of("All"));

            // 앱에서 분기 처리를 위한 추가 데이터
            Map<String, Object> data = new HashMap<>();
            data.put("type", "NOTICE");
            body.put("data", data);

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
