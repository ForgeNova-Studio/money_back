package com.moneyflow.talmo.service;

import com.moneyflow.talmo.config.KakaoConfig;
import com.moneyflow.talmo.domain.TalmoUser;
import com.moneyflow.talmo.repository.TalmoUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 카카오톡 메시지 발송 서비스
 * - refresh_token으로 access_token 갱신
 * - "나에게 보내기" API로 메시지 발송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoMessageService {

    private final KakaoConfig kakaoConfig;
    private final TalmoUserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 특정 유저에게 카카오톡 메시지 발송
     */
    @Transactional
    public boolean sendMessage(TalmoUser user, String message) {
        if (!user.hasKakaoToken()) {
            log.warn("카카오 토큰 없음 - userId: {}, name: {}", user.getId(), user.getName());
            return false;
        }

        // 1. refresh_token으로 access_token 갱신
        String accessToken = refreshAccessToken(user);
        if (accessToken == null) {
            log.error("토큰 갱신 실패 - userId: {}", user.getId());
            return false;
        }

        // 2. "나에게 보내기" API 호출
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 텍스트 메시지 템플릿
            String templateObject = """
                    {
                        "object_type": "text",
                        "text": "%s",
                        "link": {
                            "web_url": "https://talmo.your-domain.com",
                            "mobile_web_url": "https://talmo.your-domain.com"
                        },
                        "button_title": "지금 풀러가기 🔥"
                    }
                    """.formatted(escapeJson(message));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("template_object", templateObject);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(
                    "https://kapi.kakao.com/v2/api/talk/memo/default/send",
                    request,
                    Map.class);

            log.info("카카오 메시지 발송 성공 - userId: {}, name: {}", user.getId(), user.getName());
            return true;

        } catch (Exception e) {
            log.error("카카오 메시지 발송 실패 - userId: {}", user.getId(), e);
            return false;
        }
    }

    /**
     * refresh_token으로 access_token 갱신
     * - 새 refresh_token이 오면 DB에 업데이트
     */
    private String refreshAccessToken(TalmoUser user) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", kakaoConfig.getRestApiKey());
            body.add("refresh_token", user.getKakaoRefreshToken());

            if (kakaoConfig.getClientSecret() != null && !kakaoConfig.getClientSecret().isEmpty()) {
                body.add("client_secret", kakaoConfig.getClientSecret());
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    "https://kauth.kakao.com/oauth/token",
                    request,
                    Map.class);

            if (response == null)
                return null;

            String newAccessToken = (String) response.get("access_token");
            String newRefreshToken = (String) response.get("refresh_token");

            // refresh_token이 갱신되었으면 DB 업데이트
            if (newRefreshToken != null) {
                user.updateKakaoTokens(newAccessToken, newRefreshToken);
            } else {
                user.updateKakaoTokens(newAccessToken, user.getKakaoRefreshToken());
            }
            userRepository.save(user);

            return newAccessToken;

        } catch (Exception e) {
            log.error("토큰 갱신 실패 - userId: {}", user.getId(), e);
            return null;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
