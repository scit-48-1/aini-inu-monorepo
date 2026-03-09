package scit.ainiinu.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.notification.dto.response.NotificationListResponse;
import scit.ainiinu.notification.service.NotificationService;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<NotificationListResponse>>> getNotifications(
            @CurrentMember Long memberId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SliceResponse<NotificationListResponse> response = notificationService.getNotifications(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "안읽은 알림 수 조회", description = "로그인한 사용자의 안읽은 알림 수를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @CurrentMember Long memberId
    ) {
        long count = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @CurrentMember Long memberId,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(memberId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @CurrentMember Long memberId
    ) {
        notificationService.markAllAsRead(memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
