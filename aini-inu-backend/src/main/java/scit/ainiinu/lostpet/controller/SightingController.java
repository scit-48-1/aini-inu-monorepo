package scit.ainiinu.lostpet.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.lostpet.dto.SightingCreateRequest;
import scit.ainiinu.lostpet.dto.SightingResponse;
import scit.ainiinu.lostpet.service.SightingService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sightings")
@Tag(name = "Lost Pets", description = "실종 반려견 API")
@SecurityRequirement(name = "bearerAuth")
public class SightingController {

    private final SightingService sightingService;

    @PostMapping
    @Operation(summary = "제보 등록", description = "실종견 목격 제보를 등록합니다.")
    public ResponseEntity<ApiResponse<SightingResponse>> create(
            @CurrentMember Long memberId,
            @Valid @RequestBody SightingCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(sightingService.create(memberId, request)));
    }
}
