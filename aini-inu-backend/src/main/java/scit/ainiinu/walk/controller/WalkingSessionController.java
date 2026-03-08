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
@Tag(name = "Walking Sessions", description = "산책 상태 관리 API — 산책 시작/종료, 하트비트 갱신, 산책중인 유저 목록 조회")
@SecurityRequirement(name = "bearerAuth")
public class WalkingSessionController {

    private final WalkingSessionService walkingSessionService;

    @PostMapping("/start")
    @Operation(
            summary = "산책 시작",
            description = "새로운 산책 세션을 시작합니다. 이미 활성 세션이 있으면 409 에러를 반환합니다. "
                    + "시작 후 클라이언트는 주기적으로 heartbeat를 전송해야 합니다. "
                    + "5분 이상 heartbeat가 없으면 서버가 자동으로 세션을 종료합니다."
    )
    public ResponseEntity<ApiResponse<WalkingSessionResponse>> startSession(
            @CurrentMember Long memberId
    ) {
        WalkingSessionResponse response = walkingSessionService.startSession(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/heartbeat")
    @Operation(
            summary = "산책 하트비트",
            description = "활성 산책 세션의 마지막 하트비트 시각을 갱신합니다. "
                    + "클라이언트는 1~2분 간격으로 호출해야 합니다. "
                    + "활성 세션이 없으면 404 에러를 반환합니다."
    )
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @CurrentMember Long memberId
    ) {
        walkingSessionService.heartbeat(memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/stop")
    @Operation(
            summary = "산책 종료",
            description = "활성 산책 세션을 정상 종료합니다. "
                    + "활성 세션이 없으면 404 에러를 반환합니다. "
                    + "종료 후 다시 start를 호출하면 새 세션이 생성됩니다."
    )
    public ResponseEntity<ApiResponse<Void>> stopSession(
            @CurrentMember Long memberId
    ) {
        walkingSessionService.stopSession(memberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/active")
    @Operation(
            summary = "산책중인 유저 목록",
            description = "현재 ACTIVE 상태의 산책 세션을 가진 전체 유저 목록을 조회합니다. "
                    + "각 유저의 닉네임, 프로필 이미지, 매너 온도, 산책 시작 시각이 포함됩니다."
    )
    public ResponseEntity<ApiResponse<List<WalkingUserResponse>>> getActiveWalkers(
            @CurrentMember Long memberId
    ) {
        List<WalkingUserResponse> response = walkingSessionService.getActiveWalkers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @Operation(
            summary = "내 산책 세션 조회",
            description = "현재 사용자의 활성 산책 세션을 조회합니다. "
                    + "활성 세션이 없으면 data가 null로 반환됩니다 (에러가 아닙니다)."
    )
    public ResponseEntity<ApiResponse<WalkingSessionResponse>> getMySession(
            @CurrentMember Long memberId
    ) {
        WalkingSessionResponse response = walkingSessionService.getMySession(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
