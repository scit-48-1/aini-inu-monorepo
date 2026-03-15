package scit.ainiinu.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.dashboard.dto.response.DashboardSummaryResponse;
import scit.ainiinu.dashboard.service.DashboardService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "대시보드 집계 API")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "대시보드 요약 조회", description = "대시보드 초기 렌더에 필요한 요약 데이터를 한 번에 조회합니다.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @CurrentMember Long memberId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radius
    ) {
        DashboardSummaryResponse response = dashboardService.getSummary(memberId, latitude, longitude, radius);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
