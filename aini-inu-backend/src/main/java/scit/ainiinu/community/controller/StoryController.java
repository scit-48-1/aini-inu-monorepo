package scit.ainiinu.community.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.community.dto.StoryGroupResponse;
import scit.ainiinu.community.service.StoryService;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
@Tag(name = "Community", description = "커뮤니티 API")
@SecurityRequirement(name = "bearerAuth")
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    @Operation(summary = "스토리 목록 조회", description = "팔로워 대상 24시간 산책일기 스토리 그룹(회원별)을 조회합니다.")
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
                    description = "서버 고정 정렬(max(createdAt) desc, memberId desc)로 처리되며 sort 파라미터는 무시됩니다. "
                            + "요청 형식 예: sort=createdAt,desc&sort=memberId,desc (JSON 배열 형식 미지원)",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "createdAt,desc"))
            )
    })
    public ResponseEntity<ApiResponse<SliceResponse<StoryGroupResponse>>> getStories(
            @CurrentMember Long memberId,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        SliceResponse<StoryGroupResponse> response = storyService.getStories(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
