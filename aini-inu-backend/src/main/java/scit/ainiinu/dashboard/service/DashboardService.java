package scit.ainiinu.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.service.ChatRoomService;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.dashboard.dto.response.DashboardSummaryResponse;
import scit.ainiinu.member.dto.response.ActivityStatsResponse;
import scit.ainiinu.member.service.MemberService;
import scit.ainiinu.pet.dto.response.PetResponse;
import scit.ainiinu.pet.service.PetService;
import scit.ainiinu.walk.dto.response.ThreadHotspotResponse;
import scit.ainiinu.walk.dto.response.ThreadSummaryResponse;
import scit.ainiinu.walk.service.WalkThreadService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int DASHBOARD_THREADS_SIZE = 3;
    private static final int RECENT_FRIENDS_SIZE = 5;
    private static final int PENDING_REVIEWS_SIZE = 20;
    private static final int HOTSPOT_HOURS = 24;
    private static final double DEFAULT_RADIUS_KM = 5.0;

    private final MemberService memberService;
    private final WalkThreadService walkThreadService;
    private final PetService petService;
    private final ChatRoomService chatRoomService;

    public DashboardSummaryResponse getSummary(Long memberId, Double latitude, Double longitude, Double radiusKm) {
        Double effectiveRadius = latitude != null && longitude != null
                ? (radiusKm != null ? radiusKm : DEFAULT_RADIUS_KM)
                : null;

        ActivityStatsResponse activityStats = memberService.getActivityStats(memberId);
        List<ThreadHotspotResponse> hotspots = walkThreadService.getHotspots(HOTSPOT_HOURS);
        SliceResponse<ThreadSummaryResponse> threads = walkThreadService.getThreads(
                memberId,
                PageRequest.of(0, DASHBOARD_THREADS_SIZE),
                null,
                null,
                latitude,
                longitude,
                effectiveRadius
        );
        List<PetResponse> myPets = petService.getUserPets(memberId);

        return DashboardSummaryResponse.builder()
                .activityStats(activityStats)
                .hotspots(hotspots)
                .threads(threads.getContent())
                .myPets(myPets)
                .recentFriends(chatRoomService.getDashboardRecentFriends(memberId, RECENT_FRIENDS_SIZE))
                .pendingReviews(chatRoomService.getDashboardPendingReviews(memberId, PENDING_REVIEWS_SIZE))
                .build();
    }
}
