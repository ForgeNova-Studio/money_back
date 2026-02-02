package com.moneyflow.domain.notification;

import com.moneyflow.domain.user.User;
import com.moneyflow.domain.user.UserRepository;
import com.moneyflow.dto.request.NotificationRequest;
import com.moneyflow.dto.response.NotificationResponse;
import com.moneyflow.exception.ResourceNotFoundException;
import com.moneyflow.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * 알림 생성 (관리자용)
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new ResourceNotFoundException("수신자를 찾을 수 없습니다"));

        Notification notification = Notification.builder()
                .user(targetUser)
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", request.getTargetUserId(), saved.getNotificationId());

        return toResponse(saved);
    }

    /**
     * 내 알림 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByUserUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("알림을 찾을 수 없습니다"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("해당 알림에 접근할 권한이 없습니다");
        }

        if (!notification.getIsRead()) {
            notification.markAsRead();
            log.info("Marked notification {} as read", notificationId);
        }
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
