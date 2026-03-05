package scit.ainiinu.lostpet.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeResponse;
import scit.ainiinu.lostpet.dto.LostPetCreateRequest;
import scit.ainiinu.lostpet.dto.LostPetDetailResponse;
import scit.ainiinu.lostpet.dto.LostPetMatchApproveRequest;
import scit.ainiinu.lostpet.dto.LostPetMatchCandidateResponse;
import scit.ainiinu.lostpet.dto.LostPetMatchResponse;
import scit.ainiinu.lostpet.dto.LostPetResponse;
import scit.ainiinu.lostpet.dto.LostPetSummaryResponse;
import scit.ainiinu.lostpet.service.LostPetAnalyzeService;
import scit.ainiinu.lostpet.service.LostPetMatchApprovalService;
import scit.ainiinu.lostpet.service.LostPetMatchQueryService;
import scit.ainiinu.lostpet.service.LostPetService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lost-pets")
@Tag(name = "Lost Pets", description = "실종 반려견 API")
@SecurityRequirement(name = "bearerAuth")
public class LostPetController {

    private final LostPetService lostPetService;
    private final LostPetAnalyzeService lostPetAnalyzeService;
    private final LostPetMatchQueryService lostPetMatchQueryService;
    private final LostPetMatchApprovalService lostPetMatchApprovalService;

    @PostMapping
    @Operation(summary = "실종 신고 생성", description = "반려견 실종 신고를 생성합니다.")
    public ResponseEntity<ApiResponse<LostPetResponse>> create(
            @CurrentMember Long memberId,
            @Valid @RequestBody LostPetCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(lostPetService.create(memberId, request)));
    }

    @GetMapping
    @Operation(summary = "실종 신고 목록 조회", description = "내 실종 신고 목록을 상태 필터와 함께 조회합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<LostPetSummaryResponse>>> list(
            @CurrentMember Long memberId,
            @RequestParam(name = "status", required = false) LostPetReportStatus status,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(SliceResponse.of(lostPetService.list(memberId, status, pageable))));
    }

    @GetMapping("/{lostPetId}")
    @Operation(summary = "실종 신고 상세 조회", description = "lostPetId 기준 실종 신고 상세를 조회합니다.")
    public ResponseEntity<ApiResponse<LostPetDetailResponse>> detail(
            @CurrentMember Long memberId,
            @PathVariable("lostPetId") Long lostPetId
    ) {
        return ResponseEntity.ok(ApiResponse.success(lostPetService.detail(memberId, lostPetId)));
    }

    @PostMapping("/analyze")
    @Operation(summary = "실종 신고 분석", description = "실종 신고 정보 기반 AI 후보 탐색 세션을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "분석 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "타인 신고 접근 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI 분석 실패")
    })
    public ResponseEntity<ApiResponse<LostPetAnalyzeResponse>> analyze(
            @CurrentMember Long memberId,
            @Valid @RequestBody LostPetAnalyzeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(lostPetAnalyzeService.analyze(memberId, request)));
    }

    @GetMapping("/{lostPetId}/match")
    @Operation(summary = "매칭 후보 조회", description = "세션 기반 매칭 후보를 조회합니다. sessionId 미지정 시 최신 유효 세션 기준입니다.")
    public ResponseEntity<ApiResponse<SliceResponse<LostPetMatchCandidateResponse>>> matchCandidates(
            @CurrentMember Long memberId,
            @PathVariable("lostPetId") Long lostPetId,
            @RequestParam(name = "sessionId", required = false) Long sessionId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(SliceResponse.of(
                lostPetMatchQueryService.findCandidates(lostPetId, memberId, sessionId, pageable)
        )));
    }

    @PostMapping("/{lostPetId}/match")
    @Operation(summary = "매칭 후보 승인", description = "세션 후보를 승인하고 매치/채팅 연계를 시도합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "타인 신고 접근 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "후보/상태 충돌"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "세션 만료")
    })
    public ResponseEntity<ApiResponse<LostPetMatchResponse>> approveMatch(
            @CurrentMember Long memberId,
            @PathVariable("lostPetId") Long lostPetId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody LostPetMatchApproveRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                lostPetMatchApprovalService.approve(lostPetId, memberId, request, authorizationHeader)
        ));
    }
}
