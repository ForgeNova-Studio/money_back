package com.moneyflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Kakao OAuth 검증 서비스
 * Kakao Access Token을 검증하고 사용자 정보를 추출합니다.
 */
@Service
public class KakaoOAuthService {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kakao Access Token 검증 및 사용자 정보 추출
     */
    public KakaoUserInfo verifyAccessToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String providerId = rootNode.path("id").asText();
            JsonNode kakaoAccount = rootNode.path("kakao_account");
            String email = kakaoAccount.path("email").asText();

            JsonNode profile = kakaoAccount.path("profile");
            String nickname = profile.path("nickname").asText();
            String profileImageUrl = profile.path("profile_image_url").asText();

            if (providerId == null || providerId.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN);
            }

            return new KakaoUserInfo(providerId, email, nickname, profileImageUrl);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    public record KakaoUserInfo(String providerId, String email, String nickname, String profileImage) {
    }
}
