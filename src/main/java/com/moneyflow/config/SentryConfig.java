package com.moneyflow.config;

import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.User;
import io.sentry.spring.jakarta.SentryExceptionResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sentry 에러 모니터링 설정
 * 
 * 무료 플랜 최적화:
 * - 프로덕션에서만 활성화 (DSN이 비어있으면 비활성화)
 * - 중복 에러 필터링 (5분 내 같은 에러 재전송 방지)
 * - 사용자 컨텍스트 자동 설정
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "sentry.dsn", matchIfMissing = false)
public class SentryConfig {

    @Value("${sentry.dsn:}")
    private String sentryDsn;

    // 중복 에러 필터링을 위한 캐시 (에러 fingerprint -> 마지막 전송 시간)
    private static final Map<String, Long> recentErrors = new ConcurrentHashMap<>();
    private static final long DUPLICATE_THRESHOLD_MS = 5 * 60 * 1000; // 5분

    /**
     * Sentry 초기화 후 BeforeSend 콜백 설정
     */
    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            // 중복 에러 필터링
            if (isDuplicateError(event)) {
                log.debug("[Sentry] Duplicate error filtered: {}", event.getEventId());
                return null;
            }

            // 사용자 컨텍스트 추가
            addUserContext(event);

            return event;
        };
    }

    /**
     * 중복 에러 체크 (5분 내 같은 에러는 필터링)
     */
    private boolean isDuplicateError(SentryEvent event) {
        String fingerprint = generateFingerprint(event);
        long now = System.currentTimeMillis();

        Long lastSent = recentErrors.get(fingerprint);
        if (lastSent != null && (now - lastSent) < DUPLICATE_THRESHOLD_MS) {
            return true;
        }

        recentErrors.put(fingerprint, now);

        // 오래된 항목 정리 (메모리 누수 방지)
        if (recentErrors.size() > 1000) {
            recentErrors.entrySet().removeIf(
                    entry -> (now - entry.getValue()) > DUPLICATE_THRESHOLD_MS);
        }

        return false;
    }

    /**
     * 에러 fingerprint 생성 (에러 타입 + 메시지 + 첫번째 스택 프레임)
     */
    private String generateFingerprint(SentryEvent event) {
        StringBuilder sb = new StringBuilder();

        if (event.getThrowable() != null) {
            sb.append(event.getThrowable().getClass().getName());
            sb.append(":");
            sb.append(event.getThrowable().getMessage());

            StackTraceElement[] stackTrace = event.getThrowable().getStackTrace();
            if (stackTrace.length > 0) {
                sb.append("@");
                sb.append(stackTrace[0].toString());
            }
        } else if (event.getMessage() != null) {
            sb.append(event.getMessage().getFormatted());
        }

        return sb.toString();
    }

    /**
     * 현재 인증된 사용자 정보를 Sentry 이벤트에 추가
     */
    private void addUserContext(SentryEvent event) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                User user = new User();
                user.setId(auth.getName()); // userId (UUID)
                event.setUser(user);
            }
        } catch (Exception e) {
            // 사용자 컨텍스트 추가 실패 시 무시
        }
    }
}
