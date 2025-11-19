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
 * Naver OAuth 검증 서비스
 * Naver Access Token을 검증하고 사용자 정보를 추출합니다.
 */
@Service
public class NaverOAuthService {

    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Naver Access Token 검증 및 사용자 정보 추출
     *
     * @param accessToken Naver Access Token
     * @return 사용자 정보 (이메일, 이름, providerId)
     * @throws IllegalArgumentException 유효하지 않은 토큰
     */
    public NaverUserInfo verifyAccessToken(String accessToken) {
        try {
            // HTTP 헤더 설정 (Bearer Token)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Naver 사용자 정보 API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    NAVER_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String resultCode = rootNode.path("resultcode").asText();

            // 결과 코드 확인 (00: 성공)
            if (!"00".equals(resultCode)) {
                String message = rootNode.path("message").asText();
                throw new IllegalArgumentException("Naver API error: " + message);
            }

            // 사용자 정보 추출
            JsonNode responseNode = rootNode.path("response");
            String providerId = responseNode.path("id").asText();
            String email = responseNode.path("email").asText();
            String name = responseNode.path("name").asText();
            String nickname = responseNode.path("nickname").asText();
            String profileImage = responseNode.path("profile_image").asText();

            // 이메일 또는 ID가 없으면 예외
            if (providerId == null || providerId.isEmpty()) {
                throw new IllegalArgumentException("Invalid Naver access token: missing provider ID");
            }

            // 이름이 없으면 닉네임 사용
            if (name == null || name.isEmpty()) {
                name = nickname;
            }

            return new NaverUserInfo(providerId, email, name, profileImage);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Naver access token: " + e.getMessage(), e);
        }
    }

    /**
     * Naver 사용자 정보
     */
    public record NaverUserInfo(
            String providerId,
            String email,
            String name,
            String profileImage
    ) {}
}
