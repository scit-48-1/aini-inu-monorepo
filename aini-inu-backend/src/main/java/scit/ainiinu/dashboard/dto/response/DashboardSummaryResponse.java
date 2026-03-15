package scit.ainiinu.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.member.dto.response.ActivityStatsResponse;
import scit.ainiinu.pet.dto.response.PetResponse;
import scit.ainiinu.walk.dto.response.ThreadHotspotResponse;
import scit.ainiinu.walk.dto.response.ThreadSummaryResponse;

import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {

    @Schema(description = "활동 통계")
    private ActivityStatsResponse activityStats;

    @Schema(description = "핫스팟 목록")
    private List<ThreadHotspotResponse> hotspots;

    @Schema(description = "동네 소식 목록")
    private List<ThreadSummaryResponse> threads;

    @Schema(description = "내 반려견 목록")
    private List<PetResponse> myPets;

    @Schema(description = "최근 산책 친구 목록")
    private List<DashboardRecentFriendResponse> recentFriends;

    @Schema(description = "리뷰 작성 대기 목록")
    private List<DashboardPendingReviewResponse> pendingReviews;
}
