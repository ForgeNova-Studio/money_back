package com.moneyflow.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moneyflow.exception.BusinessException;
import com.moneyflow.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Google OAuth 검증 서비스
 * Google ID Token을 검증하고 사용자 정보를 추출합니다.
 */
@Service
@Slf4j
public class GoogleOAuthService {

    @Value("${oauth.google.client-id:}")
    private String clientId;

    @Value("${oauth.google.ios-client-id:}")
    private String iosClientId;

    /**
     * Google ID Token 검증 및 사용자 정보 추출
     */
    public GoogleUserInfo verifyIdToken(String idToken) throws GeneralSecurityException, IOException {
        log.debug("Verifying Google ID Token...");

        List<String> clientIds = new ArrayList<>();
        clientIds.add(clientId);
        if (iosClientId != null && !iosClientId.isEmpty()) {
            clientIds.add(iosClientId);
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .build();

        GoogleIdToken googleIdToken = verifier.verify(idToken);
        if (googleIdToken == null) {
            log.error(
                    "Google ID token verification failed - token may be expired, invalid, or issued for different client");
            throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN);
        }

        log.debug("Google ID token verified successfully");

        Payload payload = googleIdToken.getPayload();
        String providerId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        return new GoogleUserInfo(providerId, email, name, pictureUrl);
    }

    public record GoogleUserInfo(String providerId, String email, String name, String pictureUrl) {
    }
}
