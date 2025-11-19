package com.moneyflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     *
     * @param accessToken Kakao Access Token
     * @return 사용자 정보 (이메일, 닉네임, providerId)
     * @throws IllegalArgumentException 유효하지 않은 토큰
     */
    public KakaoUserInfo verifyAccessToken(String accessToken) {
        try {
            // HTTP 헤더 설정 (Bearer Token)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Kakao 사용자 정보 API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            // 사용자 정보 추출
            String providerId = rootNode.path("id").asText();
            JsonNode kakaoAccount = rootNode.path("kakao_account");
            String email = kakaoAccount.path("email").asText();

            // 프로필 정보 추출
            JsonNode profile = kakaoAccount.path("profile");
            String nickname = profile.path("nickname").asText();
            String profileImageUrl = profile.path("profile_image_url").asText();

            // ID가 없으면 예외
            if (providerId == null || providerId.isEmpty()) {
                throw new IllegalArgumentException("Invalid Kakao access token: missing provider ID");
            }

            return new KakaoUserInfo(providerId, email, nickname, profileImageUrl);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Kakao access token: " + e.getMessage(), e);
        }
    }

    /**
     * Kakao 사용자 정보
     */
    public record KakaoUserInfo(
            String providerId,
            String email,
            String nickname,
            String profileImage
    ) {}
}
