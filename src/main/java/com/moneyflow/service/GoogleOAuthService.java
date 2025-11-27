package com.moneyflow.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Google OAuth 검증 서비스
 * Google ID Token을 검증하고 사용자 정보를 추출합니다.
 */
@Service
public class GoogleOAuthService {

    @Value("${oauth.google.client-id:}")
    private String clientId;

    @Value("${oauth.google.ios-client-id:}")
    private String iosClientId;

    /**
     * Google ID Token 검증 및 사용자 정보 추출
     *
     * @param idToken Google ID Token
     * @return 사용자 정보 (이메일, 이름, providerId)
     * @throws GeneralSecurityException 토큰 검증 실패
     * @throws IOException 네트워크 오류
     * @throws IllegalArgumentException 유효하지 않은 토큰
     */
    public GoogleUserInfo verifyIdToken(String idToken) throws GeneralSecurityException, IOException {
        // 디버깅: 토큰 앞 10자리만 로깅 (보안)
        System.out.println("[DEBUG] Google ID Token (first 50 chars): " + idToken.substring(0, Math.min(50, idToken.length())) + "...");

        // 웹, iOS, Android 등 모든 Client ID를 포함
        java.util.List<String> clientIds = new java.util.ArrayList<>();
        clientIds.add(clientId);
        if (iosClientId != null && !iosClientId.isEmpty()) {
            clientIds.add(iosClientId);
        }

        System.out.println("[DEBUG] Allowed Client IDs: " + clientIds);

        // Google ID Token Verifier 생성
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(clientIds)
                .build();

        // ID Token 검증
        GoogleIdToken googleIdToken = verifier.verify(idToken);
        if (googleIdToken == null) {
            System.err.println("[ERROR] Google ID token verification returned null");
            System.err.println("[ERROR] This usually means:");
            System.err.println("  1. Token was issued for different Client ID");
            System.err.println("  2. Token expired");
            System.err.println("  3. Token format is invalid");
            System.err.println("  4. Network issue connecting to Google API");
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        System.out.println("[DEBUG] Google ID token verified successfully");

        // Payload에서 사용자 정보 추출
        Payload payload = googleIdToken.getPayload();
        String providerId = payload.getSubject();  // Google User ID
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        return new GoogleUserInfo(providerId, email, name, pictureUrl);
    }

    /**
     * Google 사용자 정보
     */
    public record GoogleUserInfo(
            String providerId,
            String email,
            String name,
            String pictureUrl
    ) {}
}
