package scit.ainiinu.timeline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.timeline.dto.request.TimelineSettingsRequest;
import scit.ainiinu.timeline.dto.response.TimelineEventResponse;
import scit.ainiinu.timeline.dto.response.TimelineSettingsResponse;
import scit.ainiinu.timeline.service.TimelineService;

@Tag(name = "Timeline", description = "활동 타임라인 API — 회원의 산책/커뮤니티/실종 신고 등 활동 내역 조회 및 공개 설정 관리")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/members/{memberId}/timeline")
    @Operation(
            summary = "회원 타임라인 조회",
            description = "특정 회원의 활동 타임라인을 최신순으로 조회합니다. "
                    + "본인의 타임라인은 항상 조회 가능하며, 타인의 타임라인은 공개 설정이 켜져 있을 때만 조회할 수 있습니다. "
                    + "비공개 타임라인을 타인이 조회하면 403 에러를 반환합니다. "
                    + "삭제된 이벤트는 조회 결과에 포함되지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "타임라인 조회 성공"),
            @ApiResponse(responseCode = "403", description = "비공개 타임라인 — 타인의 비공개 타임라인에 접근한 경우", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    @Parameters({
            @Parameter(
                    name = "page",
                    in = ParameterIn.QUERY,
                    description = "페이지 번호 (0부터 시작)",
                    schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
            ),
            @Parameter(
                    name = "size",
                    in = ParameterIn.QUERY,
                    description = "페이지당 이벤트 수",
                    schema = @Schema(type = "integer", defaultValue = "20", minimum = "1")
            ),
            @Parameter(
                    name = "sort",
                    in = ParameterIn.QUERY,
                    description = "서버 고정 정렬(occurredAt desc)로 처리되며 sort 파라미터는 무시됩니다.",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "occurredAt,desc"))
            )
    })
    public ResponseEntity<scit.ainiinu.common.response.ApiResponse<SliceResponse<TimelineEventResponse>>> getTimeline(
            @CurrentMember Long requesterId,
            @Parameter(description = "조회 대상 회원 ID", example = "1", required = true)
            @PathVariable Long memberId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(scit.ainiinu.common.response.ApiResponse.success(
                timelineService.getTimeline(requesterId, memberId, pageable)));
    }

    @PatchMapping("/members/me/timeline/settings")
    @Operation(
            summary = "타임라인 공개 설정 변경",
            description = "내 타임라인의 공개/비공개 설정을 변경합니다. "
                    + "비공개로 설정하면 타인이 내 타임라인을 조회할 수 없습니다. "
                    + "본인은 설정과 관계없이 항상 자신의 타임라인을 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "설정 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 — isTimelinePublic이 null인 경우", content = @Content),
            @ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음", content = @Content)
    })
    public ResponseEntity<scit.ainiinu.common.response.ApiResponse<TimelineSettingsResponse>> updateSettings(
            @CurrentMember Long memberId,
            @Valid @RequestBody TimelineSettingsRequest request) {
        return ResponseEntity.ok(scit.ainiinu.common.response.ApiResponse.success(
                timelineService.updateSettings(memberId, request)));
    }
}
