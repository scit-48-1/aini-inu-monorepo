package scit.ainiinu.timeline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.timeline.dto.request.TimelineSettingsRequest;
import scit.ainiinu.timeline.dto.response.TimelineEventResponse;
import scit.ainiinu.timeline.dto.response.TimelineSettingsResponse;
import scit.ainiinu.timeline.service.TimelineService;

@Tag(name = "Timeline", description = "타임라인 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/members/{memberId}/timeline")
    @Operation(summary = "타임라인 조회", description = "특정 회원의 활동 타임라인을 조회합니다. 비공개 타임라인은 본인만 조회 가능합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<TimelineEventResponse>>> getTimeline(
            @CurrentMember Long requesterId,
            @Parameter(description = "조회 대상 회원 ID") @PathVariable Long memberId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                timelineService.getTimeline(requesterId, memberId, pageable)));
    }

    @PatchMapping("/members/me/timeline/settings")
    @Operation(summary = "타임라인 공개 설정 변경", description = "내 타임라인 공개/비공개 설정을 변경합니다.")
    public ResponseEntity<ApiResponse<TimelineSettingsResponse>> updateSettings(
            @CurrentMember Long memberId,
            @Valid @RequestBody TimelineSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                timelineService.updateSettings(memberId, request)));
    }
}
