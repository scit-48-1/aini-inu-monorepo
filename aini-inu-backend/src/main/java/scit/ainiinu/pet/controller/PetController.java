package scit.ainiinu.pet.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.pet.dto.request.PetCreateRequest;
import scit.ainiinu.pet.dto.request.PetUpdateRequest;
import scit.ainiinu.pet.dto.response.BreedResponse;
import scit.ainiinu.pet.dto.response.MainPetChangeResponse;
import scit.ainiinu.pet.dto.response.PersonalityResponse;
import scit.ainiinu.pet.dto.response.PetResponse;
import scit.ainiinu.pet.dto.response.WalkingStyleResponse;
import scit.ainiinu.pet.service.PetService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Pets", description = "반려견 API")
@SecurityRequirement(name = "bearerAuth")
public class PetController {

    private final PetService petService;

    /**
     * 반려견 등록
     */
    @PostMapping("/pets")
    @Operation(summary = "반려견 등록", description = "현재 로그인한 회원의 반려견을 등록합니다.")
    public ResponseEntity<ApiResponse<PetResponse>> createPet(
            @CurrentMember Long memberId,
            @Valid @RequestBody PetCreateRequest request
    ) {
        PetResponse response = petService.createPet(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 반려견 정보 수정
     */
    @PatchMapping("/pets/{petId}")
    @Operation(summary = "반려견 수정", description = "등록된 반려견 정보를 부분 수정합니다.")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(
            @CurrentMember Long memberId,
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request
    ) {
        PetResponse response = petService.updatePet(memberId, petId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 반려견 삭제
     */
    @DeleteMapping("/pets/{petId}")
    @Operation(summary = "반려견 삭제", description = "등록된 반려견을 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deletePet(
            @CurrentMember Long memberId,
            @PathVariable Long petId
    ) {
        petService.deletePet(memberId, petId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 내 반려견 목록 조회
     */
    @GetMapping("/pets")
    @Operation(summary = "내 반려견 목록 조회", description = "현재 로그인한 회원의 반려견 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getMyPets(
            @CurrentMember Long memberId
    ) {
        List<PetResponse> response = petService.getUserPets(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 메인 반려견 변경
     */
    @PatchMapping("/pets/{petId}/main")
    @Operation(summary = "메인 반려견 변경", description = "대표 반려견을 변경합니다.")
    public ResponseEntity<ApiResponse<MainPetChangeResponse>> changeMainPet(
            @CurrentMember Long memberId,
            @PathVariable Long petId
    ) {
        MainPetChangeResponse response = petService.changeMainPet(memberId, petId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 견종 목록 조회
     */
    @GetMapping("/breeds")
    @Operation(summary = "견종 목록 조회", description = "서비스에서 지원하는 견종 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<BreedResponse>>> getBreeds() {
        List<BreedResponse> response = petService.getAllBreeds();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 성격 카테고리 목록 조회
     */
    @GetMapping("/personalities")
    @Operation(summary = "반려견 성격 목록 조회", description = "서비스에서 지원하는 반려견 성격 카테고리를 조회합니다.")
    public ResponseEntity<ApiResponse<List<PersonalityResponse>>> getPersonalities() {
        List<PersonalityResponse> response = petService.getAllPersonalities();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 산책 스타일 목록 조회
     */
    @GetMapping("/walking-styles")
    @Operation(summary = "산책 스타일 목록 조회", description = "서비스에서 지원하는 산책 스타일 카테고리를 조회합니다.")
    public ResponseEntity<ApiResponse<List<WalkingStyleResponse>>> getWalkingStyles() {
        List<WalkingStyleResponse> response = petService.getAllWalkingStyles();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
