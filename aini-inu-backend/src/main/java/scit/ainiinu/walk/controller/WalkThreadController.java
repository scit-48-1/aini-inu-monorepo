package scit.ainiinu.walk.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.walk.dto.request.ThreadApplyRequest;
import scit.ainiinu.walk.dto.request.ThreadCreateRequest;
import scit.ainiinu.walk.dto.request.ThreadPatchRequest;
import scit.ainiinu.walk.dto.response.ThreadApplyResponse;
import scit.ainiinu.walk.dto.response.ThreadHotspotResponse;
import scit.ainiinu.walk.dto.response.ThreadMapResponse;
import scit.ainiinu.walk.dto.response.ThreadResponse;
import scit.ainiinu.walk.dto.response.ThreadSummaryResponse;
import scit.ainiinu.walk.service.WalkThreadService;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Walk Threads", description = "산책 모집 API")
@SecurityRequirement(name = "bearerAuth")
public class WalkThreadController {

    private final WalkThreadService walkThreadService;

    @PostMapping("/threads")
    @Operation(summary = "산책 모집글 생성", description = "새로운 산책 모집글을 생성합니다.")
    public ResponseEntity<ApiResponse<ThreadResponse>> createThread(
            @CurrentMember Long memberId,
            @Valid @RequestBody ThreadCreateRequest request
    ) {
        ThreadResponse response = walkThreadService.createThread(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/threads")
    @Operation(summary = "산책 모집글 목록 조회", description = "산책 모집글을 Slice 형태로 조회합니다.")
    @Parameters({
            @Parameter(
                    name = "page",
                    in = ParameterIn.QUERY,
                    description = "페이지 번호(0부터 시작)",
                    schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
            ),
            @Parameter(
                    name = "size",
                    in = ParameterIn.QUERY,
                    description = "페이지 크기",
                    schema = @Schema(type = "integer", defaultValue = "20", minimum = "1")
            ),
            @Parameter(
                    name = "sort",
                    in = ParameterIn.QUERY,
                    description = "서버 고정 정렬(createdAt desc, id desc)로 처리되며 sort 파라미터는 무시됩니다. "
                            + "요청 형식 예: sort=createdAt,desc&sort=id,desc (JSON 배열 형식 미지원)",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "createdAt,desc"))
            )
    })
    public ResponseEntity<ApiResponse<SliceResponse<ThreadSummaryResponse>>> getThreads(
            @CurrentMember Long memberId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radius
    ) {
        SliceResponse<ThreadSummaryResponse> response = walkThreadService.getThreads(memberId, pageable, startDate, endDate, latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/threads/map")
    @Operation(summary = "지도용 모집글 조회", description = "좌표/반경 기준으로 지도 노출용 모집글을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ThreadMapResponse>>> getMapThreads(
            @CurrentMember Long memberId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5") Double radius,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ThreadMapResponse> response = walkThreadService.getMapThreads(memberId, latitude, longitude, radius, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/threads/my/active")
    @Operation(summary = "내 활성 모집글 조회", description = "현재 사용자의 활성(만료되지 않은 RECRUITING) 모집글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ThreadSummaryResponse>>> getMyActiveThread(
            @CurrentMember Long memberId
    ) {
        List<ThreadSummaryResponse> response = walkThreadService.getMyActiveThread(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/threads/my/joined")
    @Operation(summary = "내 참여 중인 산책 조회", description = "현재 사용자가 참여 신청한 활성(만료되지 않은 RECRUITING) 산책 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ThreadSummaryResponse>>> getMyJoinedThreads(
            @CurrentMember Long memberId
    ) {
        List<ThreadSummaryResponse> response = walkThreadService.getMyJoinedThreads(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/threads/{threadId}")
    @Operation(summary = "산책 모집글 상세 조회", description = "threadId로 산책 모집글 상세를 조회합니다.")
    public ResponseEntity<ApiResponse<ThreadResponse>> getThread(
            @CurrentMember Long memberId,
            @PathVariable Long threadId
    ) {
        ThreadResponse response = walkThreadService.getThread(memberId, threadId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/threads/{threadId}")
    @Operation(summary = "산책 모집글 수정", description = "작성자만 산책 모집글을 수정할 수 있습니다.")
    public ResponseEntity<ApiResponse<ThreadResponse>> updateThread(
            @CurrentMember Long memberId,
            @PathVariable Long threadId,
            @RequestBody ThreadPatchRequest request
    ) {
        ThreadResponse response = walkThreadService.updateThread(memberId, threadId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/threads/{threadId}")
    @Operation(summary = "산책 모집글 삭제", description = "작성자만 산책 모집글을 삭제할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> deleteThread(
            @CurrentMember Long memberId,
            @PathVariable Long threadId
    ) {
        walkThreadService.deleteThread(memberId, threadId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/threads/{threadId}/apply")
    @Operation(summary = "산책 모집 신청", description = "모집글 참여를 신청합니다. 중복 신청은 멱등 처리됩니다.")
    public ResponseEntity<ApiResponse<ThreadApplyResponse>> applyThread(
            @CurrentMember Long memberId,
            @PathVariable Long threadId,
            @RequestBody ThreadApplyRequest request
    ) {
        ThreadApplyResponse response = walkThreadService.applyThread(memberId, threadId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/threads/{threadId}/apply")
    @Operation(summary = "산책 모집 신청 취소", description = "산책 모집 신청을 취소합니다.")
    public ResponseEntity<ApiResponse<Void>> cancelApplyThread(
            @CurrentMember Long memberId,
            @PathVariable Long threadId
    ) {
        walkThreadService.cancelApplyThread(memberId, threadId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/threads/hotspot")
    @Operation(summary = "산책 핫스팟 조회", description = "최근 시간대 기준 핫스팟 좌표/집계 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<List<ThreadHotspotResponse>>> getHotspots(
            @CurrentMember Long memberId,
            @RequestParam(defaultValue = "24") Integer hours
    ) {
        List<ThreadHotspotResponse> response = walkThreadService.getHotspots(hours);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
