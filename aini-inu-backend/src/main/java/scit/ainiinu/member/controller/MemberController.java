package scit.ainiinu.member.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
import scit.ainiinu.common.security.annotation.Public;
import scit.ainiinu.member.dto.request.MemberCreateRequest;
import scit.ainiinu.member.dto.request.MemberProfilePatchRequest;
import scit.ainiinu.member.dto.request.MemberSignupRequest;
import scit.ainiinu.member.dto.response.FollowStatusResponse;
import scit.ainiinu.member.dto.response.LoginResponse;
import scit.ainiinu.member.dto.response.MemberFollowResponse;
import scit.ainiinu.member.dto.response.MemberResponse;
import scit.ainiinu.member.service.AuthService;
import scit.ainiinu.member.service.MemberService;
import scit.ainiinu.pet.dto.response.PetResponse;
import scit.ainiinu.pet.service.PetService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Tag(name = "Members", description = "회원 API")
@SecurityRequirement(name = "bearerAuth")
public class MemberController {

    private final MemberService memberService;
    private final PetService petService;
    private final AuthService authService;

    /**
     * 회원가입 완료 (프로필 생성)
     * 가입 직후 추가 프로필 정보를 입력받아 가입을 완료합니다.
     */
    @PostMapping("/profile")
    @Operation(summary = "회원 프로필 생성", description = "가입 직후 프로필 정보를 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "요청 검증 실패")
    })
    public ResponseEntity<ApiResponse<MemberResponse>> createProfile(
            @CurrentMember Long memberId,
            @Valid @RequestBody MemberCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.createProfile(memberId, request)));
    }

    @Public
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일 기반 회원가입 후 토큰을 발급합니다.")
    @SecurityRequirements()
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 이메일")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> signup(
            @Valid @RequestBody MemberSignupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.signup(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 회원의 프로필을 조회합니다.")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyProfile(
            @CurrentMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMyProfile(memberId)));
    }

    @PatchMapping("/me")
    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 회원의 프로필을 부분 수정합니다.")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMyProfile(
            @CurrentMember Long memberId,
            @Valid @RequestBody MemberProfilePatchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMyProfile(memberId, request)));
    }

    @GetMapping("/search")
    @Operation(summary = "회원 검색", description = "닉네임/키워드 기반으로 회원을 검색합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<MemberResponse>>> searchMembers(
            @CurrentMember Long memberId,
            @RequestParam("q") String query,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.searchMembers(memberId, query, pageable)));
    }

    @GetMapping("/{memberId}")
    @Operation(summary = "회원 프로필 조회", description = "memberId로 특정 회원의 프로필을 조회합니다.")
    public ResponseEntity<ApiResponse<MemberResponse>> getMemberProfile(
            @PathVariable("memberId") Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMemberProfile(memberId)));
    }

    @GetMapping("/{memberId}/pets")
    @Operation(summary = "회원 반려견 목록 조회", description = "특정 회원이 등록한 반려견 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getMemberPets(
            @PathVariable("memberId") Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(petService.getUserPets(memberId)));
    }

    @GetMapping("/me/followers")
    @Operation(summary = "내 팔로워 목록 조회", description = "현재 로그인한 회원의 팔로워 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<MemberFollowResponse>>> getFollowers(
            @CurrentMember Long memberId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getFollowers(memberId, pageable)));
    }

    @GetMapping("/me/following")
    @Operation(summary = "내 팔로잉 목록 조회", description = "현재 로그인한 회원이 팔로우 중인 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<SliceResponse<MemberFollowResponse>>> getFollowing(
            @CurrentMember Long memberId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getFollowing(memberId, pageable)));
    }

    @PostMapping("/me/follows/{targetId}")
    @Operation(summary = "회원 팔로우", description = "targetId 회원을 팔로우합니다.")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> follow(
            @CurrentMember Long memberId,
            @PathVariable("targetId") Long targetId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.follow(memberId, targetId)));
    }

    @DeleteMapping("/me/follows/{targetId}")
    @Operation(summary = "회원 언팔로우", description = "targetId 회원 팔로우를 해제합니다.")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> unfollow(
            @CurrentMember Long memberId,
            @PathVariable("targetId") Long targetId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.unfollow(memberId, targetId)));
    }

    @GetMapping("/me/stats/walk")
    @Operation(summary = "산책 통계 조회", description = "현재 로그인한 회원의 산책 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<int[]>> getWalkStats(
            @CurrentMember Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getWalkStats(memberId)));
    }
}
