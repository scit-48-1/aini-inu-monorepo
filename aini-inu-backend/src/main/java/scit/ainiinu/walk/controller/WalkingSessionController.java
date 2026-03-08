package scit.ainiinu.walk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.walk.dto.response.WalkingSessionResponse;
import scit.ainiinu.walk.dto.response.WalkingUserResponse;
import scit.ainiinu.walk.service.WalkingSessionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/walking-sessions")
@Tag(name = "Walking Sessions", description = "산책 상태 API")
@SecurityRequirement(name = "bearerAuth")
public class WalkingSessionController {

    private final WalkingSessionService walkingSessionService;

    @PostMapping("/start")
    @Operation(summary = "산책 시작", description = "산책 세션을 시작합니다.")
    public ResponseEntity<ApiResponse<WalkingSessionResponse>> startSession(
            @CurrentMember Long memberId
    ) {
        WalkingSessionResponse response = walkingSessionService.startSession(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/heartbeat")
    @Operation(summary = "산책 하트비트", description = "산책 세션의 하트비트를 갱신합니다.")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @CurrentMember Long memberId
    ) {
        walkingSessionService.heartbeat(memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/stop")
    @Operation(summary = "산책 종료", description = "산책 세션을 종료합니다.")
    public ResponseEntity<ApiResponse<Void>> stopSession(
            @CurrentMember Long memberId
    ) {
        walkingSessionService.stopSession(memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/active")
    @Operation(summary = "산책중인 유저 목록", description = "현재 산책중인 유저 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<WalkingUserResponse>>> getActiveWalkers(
            @CurrentMember Long memberId
    ) {
        List<WalkingUserResponse> response = walkingSessionService.getActiveWalkers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @Operation(summary = "내 산책 세션 조회", description = "현재 사용자의 활성 산책 세션을 조회합니다.")
    public ResponseEntity<ApiResponse<WalkingSessionResponse>> getMySession(
            @CurrentMember Long memberId
    ) {
        WalkingSessionResponse response = walkingSessionService.getMySession(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
