package scit.ainiinu.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.dto.request.MemberCreateRequest;
import scit.ainiinu.member.dto.request.MemberProfilePatchRequest;
import scit.ainiinu.member.dto.response.FollowStatusResponse;
import scit.ainiinu.member.dto.response.MemberFollowResponse;
import scit.ainiinu.member.dto.response.MemberPersonalityTypeResponse;
import scit.ainiinu.member.dto.response.MemberResponse;
import scit.ainiinu.member.dto.response.WalkStatsPointResponse;
import scit.ainiinu.member.dto.response.WalkStatsResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.MemberFollow;
import scit.ainiinu.member.entity.MemberPersonality;
import scit.ainiinu.member.entity.MemberPersonalityType;
import scit.ainiinu.member.exception.MemberErrorCode;
import scit.ainiinu.member.exception.MemberException;
import scit.ainiinu.member.repository.MemberFollowRepository;
import scit.ainiinu.member.repository.MemberPersonalityRepository;
import scit.ainiinu.member.repository.MemberPersonalityTypeRepository;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.walk.repository.WalkDiaryDailyCountProjection;
import scit.ainiinu.walk.repository.WalkDiaryRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final int WALK_STATS_WINDOW_DAYS = 126;
    private static final ZoneId WALK_STATS_ZONE = ZoneId.of("Asia/Seoul");

    private final MemberRepository memberRepository;
    private final MemberPersonalityTypeRepository memberPersonalityTypeRepository;
    private final MemberPersonalityRepository memberPersonalityRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final WalkDiaryRepository walkDiaryRepository;

    /**
     * 회원가입 완료 (프로필 생성)
     *
     * @param memberId 인증된 회원 ID
     * @param request  프로필 정보
     * @return 갱신된 회원 정보
     */
    @Transactional
    public MemberResponse createProfile(Long memberId, MemberCreateRequest request) {
        Member member = findMember(memberId);

        // 닉네임 중복 검사 (현재 닉네임과 다를 경우에만)
        if (!member.getNickname().equals(request.getNickname()) &&
                memberRepository.existsByNickname(request.getNickname())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        // 회원 정보 업데이트
        member.updateProfile(
                request.getNickname(),
                request.getProfileImageUrl(),
                request.getLinkedNickname(),
                request.getPhone(),
                request.getAge(),
                request.getGender(),
                request.getMbti(),
                request.getPersonality(),
                request.getSelfIntroduction()
        );

        // 성격 유형 매핑 저장
        updateMemberPersonalities(member, request.getPersonalityTypeIds());
        return toMemberResponse(member);
    }

    public MemberResponse getMyProfile(Long memberId) {
        Member member = findMember(memberId);
        return toMemberResponse(member);
    }

    public MemberResponse getMemberProfile(Long memberId) {
        Member member = findMember(memberId);
        return toMemberResponse(member);
    }

    public SliceResponse<MemberResponse> searchMembers(Long memberId, String query, Pageable pageable) {
        findMember(memberId);

        String keyword = query == null ? "" : query.trim();
        Slice<Member> members = memberRepository
                .findByNicknameContainingIgnoreCaseOrLinkedNicknameContainingIgnoreCaseAndIdNot(
                        keyword, keyword, memberId, pageable
                );

        Slice<MemberResponse> mapped = members.map(this::toMemberResponse);
        return SliceResponse.of(mapped);
    }

    @Transactional
    public MemberResponse updateMyProfile(Long memberId, MemberProfilePatchRequest request) {
        Member member = findMember(memberId);

        if (request.getNickname() != null
                && !request.getNickname().equals(member.getNickname())
                && memberRepository.existsByNickname(request.getNickname())) {
            throw new MemberException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        member.updateProfile(
                request.getNickname(),
                request.getProfileImageUrl(),
                request.getLinkedNickname(),
                request.getPhone(),
                request.getAge(),
                request.getGender(),
                request.getMbti(),
                request.getPersonality(),
                request.getSelfIntroduction()
        );

        if (request.getPersonalityTypeIds() != null) {
            updateMemberPersonalities(member, request.getPersonalityTypeIds());
        }

        return toMemberResponse(member);
    }

    public SliceResponse<MemberFollowResponse> getFollowers(Long memberId, Pageable pageable) {
        findMember(memberId);
        Slice<MemberFollow> follows = memberFollowRepository.findAllByFollowingIdOrderByCreatedAtDesc(memberId, pageable);
        return mapFollowSlice(follows, MemberFollow::getFollowerId);
    }

    public SliceResponse<MemberFollowResponse> getFollowing(Long memberId, Pageable pageable) {
        findMember(memberId);
        Slice<MemberFollow> follows = memberFollowRepository.findAllByFollowerIdOrderByCreatedAtDesc(memberId, pageable);
        return mapFollowSlice(follows, MemberFollow::getFollowingId);
    }

    public FollowStatusResponse getFollowStatus(Long memberId, Long targetId) {
        boolean isFollowing = memberFollowRepository.existsByFollowerIdAndFollowingId(memberId, targetId);
        return new FollowStatusResponse(isFollowing);
    }

    @Transactional
    public FollowStatusResponse follow(Long memberId, Long targetId) {
        if (memberId.equals(targetId)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }

        findMember(memberId);
        findMember(targetId);

        memberFollowRepository.findByFollowerIdAndFollowingId(memberId, targetId)
                .orElseGet(() -> memberFollowRepository.save(
                        MemberFollow.builder()
                                .followerId(memberId)
                                .followingId(targetId)
                                .build()
                ));

        return new FollowStatusResponse(true);
    }

    @Transactional
    public FollowStatusResponse unfollow(Long memberId, Long targetId) {
        memberFollowRepository.findByFollowerIdAndFollowingId(memberId, targetId)
                .ifPresent(memberFollowRepository::delete);
        return new FollowStatusResponse(false);
    }

    public WalkStatsResponse getWalkStats(Long memberId) {
        findMember(memberId);

        LocalDate endDate = LocalDate.now(WALK_STATS_ZONE);
        LocalDate startDate = endDate.minusDays(WALK_STATS_WINDOW_DAYS - 1L);

        Map<LocalDate, Integer> dailyCountMap = walkDiaryRepository.countDailyWalks(memberId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        WalkDiaryDailyCountProjection::getWalkDate,
                        projection -> Math.toIntExact(projection.getWalkCount())
                ));

        List<WalkStatsPointResponse> points = new ArrayList<>(WALK_STATS_WINDOW_DAYS);
        int totalWalks = 0;
        for (int i = 0; i < WALK_STATS_WINDOW_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            int count = dailyCountMap.getOrDefault(date, 0);
            totalWalks += count;
            points.add(WalkStatsPointResponse.builder()
                    .date(date)
                    .count(count)
                    .build());
        }

        return WalkStatsResponse.builder()
                .windowDays(WALK_STATS_WINDOW_DAYS)
                .startDate(startDate)
                .endDate(endDate)
                .timezone(WALK_STATS_ZONE.getId())
                .totalWalks(totalWalks)
                .points(points)
                .build();
    }

    private List<MemberPersonalityTypeResponse> updateMemberPersonalities(Member member, List<Long> typeIds) {
        // 기존 매핑 삭제 (수정 시에도 활용 가능)
        memberPersonalityRepository.deleteByMember(member);
        
        if (typeIds == null || typeIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<MemberPersonalityType> types = memberPersonalityTypeRepository.findAllById(typeIds);
        

        List<MemberPersonality> newPersonalities = types.stream()
                .map(type -> MemberPersonality.builder()
                        .member(member)
                        .personalityType(type)
                        .build())
                .collect(Collectors.toList());

        memberPersonalityRepository.saveAll(newPersonalities);

        return types.stream()
                .map(MemberPersonalityTypeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원 타입을 반려견 보유자(PET_OWNER)로 업그레이드
     * 첫 반려견 등록 시 호출됨
     */
    @Transactional
    public void upgradeToPetOwner(Long memberId) {
        Member member = findMember(memberId);
        member.upgradeToPetOwner();
    }

    /**
     * 회원 타입을 비반려견 보유자(NON_PET_OWNER)로 다운그레이드
     * 마지막 반려견 삭제 시 호출됨
     */
    @Transactional
    public void downgradeToNonPetOwner(Long memberId) {
        Member member = findMember(memberId);
        member.downgradeToNonPetOwner();
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberResponse toMemberResponse(Member member) {
        List<MemberPersonalityTypeResponse> personalityTypeResponses = memberPersonalityRepository.findByMember(member).stream()
                .map(memberPersonality -> MemberPersonalityTypeResponse.from(memberPersonality.getPersonalityType()))
                .collect(Collectors.toList());

        return MemberResponse.from(member, personalityTypeResponses);
    }

    private SliceResponse<MemberFollowResponse> mapFollowSlice(
            Slice<MemberFollow> follows,
            Function<MemberFollow, Long> targetMemberIdExtractor
    ) {
        List<Long> memberIds = follows.getContent().stream()
                .map(targetMemberIdExtractor)
                .collect(Collectors.toList());

        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        Slice<MemberFollowResponse> mapped = follows.map(follow -> {
            Long targetMemberId = targetMemberIdExtractor.apply(follow);
            Member member = memberMap.get(targetMemberId);
            if (member == null) {
                throw new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
            }
            return MemberFollowResponse.of(member, follow.getCreatedAt());
        });

        return SliceResponse.of(mapped);
    }
}
