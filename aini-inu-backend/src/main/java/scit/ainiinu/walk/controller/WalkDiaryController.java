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
import scit.ainiinu.walk.dto.request.WalkDiaryCreateRequest;
import scit.ainiinu.walk.dto.request.WalkDiaryPatchRequest;
import scit.ainiinu.walk.dto.response.AvailableThreadResponse;
import scit.ainiinu.walk.dto.response.WalkDiaryResponse;
import scit.ainiinu.walk.service.WalkDiaryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Walk Diaries", description = "산책 일지 API")
@SecurityRequirement(name = "bearerAuth")
public class WalkDiaryController {

    private final WalkDiaryService walkDiaryService;

    @PostMapping("/walk-diaries")
    @Operation(summary = "산책 일지 생성", description = "새로운 산책 일지를 생성합니다.")
    public ResponseEntity<ApiResponse<WalkDiaryResponse>> createDiary(
            @CurrentMember Long memberId,
            @Valid @RequestBody WalkDiaryCreateRequest request
    ) {
        WalkDiaryResponse response = walkDiaryService.createDiary(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/walk-diaries/available-threads")
    @Operation(summary = "일기 작성 가능한 스레드 목록 조회", description = "완료된 산책 스레드 중 아직 일기를 작성하지 않은 스레드 목록을 반환합니다.")
    public ResponseEntity<ApiResponse<List<AvailableThreadResponse>>> getAvailableThreads(
            @CurrentMember Long memberId
    ) {
        List<AvailableThreadResponse> response = walkDiaryService.getAvailableThreads(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/walk-diaries")
    @Operation(summary = "산책 일지 목록 조회", description = "내 일지 또는 특정 회원의 일지 목록을 조회합니다.")
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
                    description = "정렬 조건입니다. sort=필드,방향 형식으로 반복 지정합니다. "
                            + "지원 예시: createdAt,desc / id,desc. "
                            + "반복 예시: sort=createdAt,desc&sort=id,desc (JSON 배열 형식 미지원)",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "createdAt,desc"))
            )
    })
    public ResponseEntity<ApiResponse<SliceResponse<WalkDiaryResponse>>> getWalkDiaries(
            @CurrentMember Long memberId,
            @RequestParam(value = "memberId", required = false) Long targetMemberId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SliceResponse<WalkDiaryResponse> response = walkDiaryService.getWalkDiaries(memberId, targetMemberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/walk-diaries/following")
    @Operation(summary = "팔로잉 일지 피드 조회", description = "팔로우한 회원의 공개 산책 일지를 조회합니다.")
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
    public ResponseEntity<ApiResponse<SliceResponse<WalkDiaryResponse>>> getFollowingDiaries(
            @CurrentMember Long memberId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SliceResponse<WalkDiaryResponse> response = walkDiaryService.getFollowingDiaries(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/walk-diaries/{diaryId}")
    @Operation(summary = "산책 일지 상세 조회", description = "diaryId로 산책 일지 상세를 조회합니다.")
    public ResponseEntity<ApiResponse<WalkDiaryResponse>> getDiary(
            @CurrentMember Long memberId,
            @PathVariable Long diaryId
    ) {
        WalkDiaryResponse response = walkDiaryService.getDiary(memberId, diaryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/walk-diaries/{diaryId}")
    @Operation(summary = "산책 일지 수정", description = "작성자만 산책 일지를 수정할 수 있습니다.")
    public ResponseEntity<ApiResponse<WalkDiaryResponse>> updateDiary(
            @CurrentMember Long memberId,
            @PathVariable Long diaryId,
            @Valid @RequestBody WalkDiaryPatchRequest request
    ) {
        WalkDiaryResponse response = walkDiaryService.updateDiary(memberId, diaryId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/walk-diaries/{diaryId}")
    @Operation(summary = "산책 일지 삭제", description = "작성자만 산책 일지를 삭제할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @CurrentMember Long memberId,
            @PathVariable Long diaryId
    ) {
        walkDiaryService.deleteDiary(memberId, diaryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
