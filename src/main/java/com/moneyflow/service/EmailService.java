package com.moneyflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;

/**
 * 이메일 발송 서비스
 * Resend API를 사용하여 이메일 발송
 */
@Service
@Slf4j
public class EmailService {

    @Value("${email.resend.api-key:re_test}")
    private String resendApiKey;

    @Value("${email.from:noreply@moneyflow.com}")
    private String fromEmail;

    @Value("${email.test-mode:true}")
    private boolean testMode;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 6자리 랜덤 인증 코드 생성 (암호학적으로 안전)
     * 테스트 모드에서는 고정 코드 "000000" 반환
     */
    public String generateVerificationCode() {
        if (testMode) {
            log.info("📧 [테스트 모드] 고정 인증 코드 사용: 000000");
            return "000000";
        }
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    /**
     * 회원가입 인증 코드 이메일 발송
     */
    public void sendSignupVerificationEmail(String toEmail, String verificationCode) {
        String subject = "[MoneyFlow] 회원가입 인증 코드";
        String htmlContent = buildSignupEmailHtml(verificationCode);
        sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * 비밀번호 재설정 인증 코드 이메일 발송
     */
    public void sendPasswordResetEmail(String toEmail, String verificationCode) {
        String subject = "[MoneyFlow] 비밀번호 재설정 인증 코드";
        String htmlContent = buildPasswordResetEmailHtml(verificationCode);
        sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Resend API를 통한 이메일 발송
     */
    private void sendEmail(String toEmail, String subject, String htmlContent) {
        if (testMode) {
            // 테스트 모드: 실제로 이메일을 발송하지 않고 로그만 출력
            log.info("📧 [테스트 모드] 이메일 발송 시뮬레이션");
            log.info("수신자: {}", toEmail);
            log.info("제목: {}", subject);
            log.info("내용: {}", htmlContent);
            return;
        }

        try {
            String requestBody = String.format(
                    """
                            {
                                "from": "%s",
                                "to": ["%s"],
                                "subject": "%s",
                                "html": "%s"
                            }
                            """,
                    fromEmail,
                    toEmail,
                    subject,
                    escapeJson(htmlContent));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("✅ 이메일 발송 성공: {}", toEmail);
            } else {
                log.error("❌ 이메일 발송 실패: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("이메일 발송에 실패했습니다: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("❌ 이메일 발송 중 오류 발생", e);
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 회원가입 인증 이메일 HTML 템플릿
     */
    private String buildSignupEmailHtml(String verificationCode) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                        <h2 style="color: #4CAF50;">MoneyFlow 회원가입</h2>
                        <p>안녕하세요!</p>
                        <p>MoneyFlow 회원가입을 위한 인증 코드입니다.</p>
                        <div style="background-color: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0; border-radius: 4px;">
                            <h1 style="color: #4CAF50; margin: 0; font-size: 36px; letter-spacing: 5px;">%s</h1>
                        </div>
                        <p>위 인증 코드를 입력하여 회원가입을 완료해주세요.</p>
                        <p style="color: #999; font-size: 14px;">인증 코드는 10분간 유효합니다.</p>
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
                        <p style="color: #999; font-size: 12px;">본인이 요청하지 않은 경우 이 이메일을 무시하셔도 됩니다.</p>
                    </div>
                </body>
                </html>
                """
                .formatted(verificationCode);
    }

    /**
     * 비밀번호 재설정 인증 이메일 HTML 템플릿
     */
    private String buildPasswordResetEmailHtml(String verificationCode) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                        <h2 style="color: #FF9800;">비밀번호 재설정</h2>
                        <p>안녕하세요!</p>
                        <p>MoneyFlow 비밀번호 재설정을 위한 인증 코드입니다.</p>
                        <div style="background-color: #f5f5f5; padding: 20px; text-align: center; margin: 20px 0; border-radius: 4px;">
                            <h1 style="color: #FF9800; margin: 0; font-size: 36px; letter-spacing: 5px;">%s</h1>
                        </div>
                        <p>위 인증 코드를 입력하여 비밀번호 재설정을 진행해주세요.</p>
                        <p style="color: #999; font-size: 14px;">인증 코드는 10분간 유효합니다.</p>
                        <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;">
                        <p style="color: #d32f2f; font-size: 14px;">⚠️ 본인이 요청하지 않은 경우 즉시 고객센터로 연락해주세요.</p>
                    </div>
                </body>
                </html>
                """
                .formatted(verificationCode);
    }

    /**
     * JSON 문자열 이스케이프 처리
     */
    private String escapeJson(String input) {
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
