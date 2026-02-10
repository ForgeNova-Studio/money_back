package com.moneyflow.domain.notification;

import com.moneyflow.dto.request.NotificationRequest;
import com.moneyflow.dto.response.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 알림 API 컨트롤러
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification", description = "알림 관리 API")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "알림 발송 (관리자용)")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request) {
        // TODO: 관리자 권한 체크 추가 필요
        NotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "days 파라미터로 최근 N일 이내 알림만 조회 가능 (예: days=7)")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer days,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, days, pageable);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "읽지 않은 알림 개수 조회")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = UUID.fromString(userDetails.getUsername());
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/send-all")
    @Operation(summary = "전체 사용자에게 알림 발송 (관리자용)")
    public ResponseEntity<Map<String, Object>> sendNotificationToAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NotificationRequest request) {

        UUID adminId = UUID.fromString(userDetails.getUsername());
        int sentCount = notificationService.sendNotificationToAll(adminId, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "sentCount", sentCount,
                "message", sentCount + "명에게 알림이 전송되었습니다"));
    }
}
