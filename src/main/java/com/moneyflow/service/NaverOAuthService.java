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
     */
    public NaverUserInfo verifyAccessToken(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    NAVER_USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String resultCode = rootNode.path("resultcode").asText();

            if (!"00".equals(resultCode)) {
                throw new BusinessException(ErrorCode.OAUTH_API_ERROR);
            }

            JsonNode responseNode = rootNode.path("response");
            String providerId = responseNode.path("id").asText();
            String email = responseNode.path("email").asText();
            String name = responseNode.path("name").asText();
            String nickname = responseNode.path("nickname").asText();
            String profileImage = responseNode.path("profile_image").asText();

            if (providerId == null || providerId.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN);
            }

            if (name == null || name.isEmpty()) {
                name = nickname;
            }

            return new NaverUserInfo(providerId, email, name, profileImage);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN);
        }
    }

    public record NaverUserInfo(String providerId, String email, String name, String profileImage) {
    }
}
