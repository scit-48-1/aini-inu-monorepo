package scit.ainiinu.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.member.dto.response.MemberPersonalityTypeResponse;
import scit.ainiinu.member.service.MemberPersonalityTypeService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member-personality-types")
@Tag(name = "Members", description = "회원 API")
@SecurityRequirement(name = "bearerAuth")
public class MemberPersonalityTypeController {

    private final MemberPersonalityTypeService personalityTypeService;

    @GetMapping
    @Operation(summary = "회원 성향 타입 목록 조회", description = "프로필 구성에 사용하는 회원 성향 타입 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MemberPersonalityTypeResponse>>> getAllPersonalityTypes() {
        List<MemberPersonalityTypeResponse> response = personalityTypeService.getAllPersonalityTypes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
