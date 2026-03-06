package scit.ainiinu.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.dto.request.MemberCreateRequest;
import scit.ainiinu.member.dto.request.MemberProfilePatchRequest;
import scit.ainiinu.member.dto.response.FollowStatusResponse;
import scit.ainiinu.member.dto.response.MemberFollowResponse;
import scit.ainiinu.member.dto.response.MemberResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.MemberFollow;
import scit.ainiinu.member.entity.MemberPersonality;
import scit.ainiinu.member.entity.MemberPersonalityType;
import scit.ainiinu.member.entity.enums.Gender;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.exception.MemberErrorCode;
import scit.ainiinu.member.exception.MemberException;
import scit.ainiinu.member.repository.MemberFollowRepository;
import scit.ainiinu.member.repository.MemberPersonalityRepository;
import scit.ainiinu.member.repository.MemberPersonalityTypeRepository;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.walk.repository.WalkDiaryDailyCountProjection;
import scit.ainiinu.walk.repository.WalkDiaryRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPersonalityTypeRepository memberPersonalityTypeRepository;

    @Mock
    private MemberPersonalityRepository memberPersonalityRepository;

    @Mock
    private MemberFollowRepository memberFollowRepository;

    @Mock
    private WalkDiaryRepository walkDiaryRepository;

    @Nested
    @DisplayName("프로필 생성")
    class CreateProfile {

        @Test
        @DisplayName("유효한 정보로 프로필을 생성하면 성공한다")
        void createProfile_withValidInfo_succeeds() {
            // given
            Long memberId = 1L;
            Member member = Member.builder()
                    .email("test@example.com")
                    .nickname("임시닉네임")
                    .build();
            ReflectionTestUtils.setField(member, "id", memberId);

            MemberCreateRequest request = new MemberCreateRequest();
            request.setNickname("새닉네임");
            request.setPhone("010-1234-5678");
            request.setAge(25);
            request.setGender(Gender.MALE);
            request.setPersonalityTypeIds(List.of(1L, 2L));

            MemberPersonalityType type1 = MemberPersonalityType.builder().id(1L).name("유형1").code("TYPE1").build();
            MemberPersonalityType type2 = MemberPersonalityType.builder().id(2L).name("유형2").code("TYPE2").build();

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("새닉네임")).willReturn(false);
            given(memberPersonalityTypeRepository.findAllById(any())).willReturn(List.of(type1, type2));
            given(memberPersonalityRepository.findByMember(member)).willReturn(List.of(
                    MemberPersonality.builder().member(member).personalityType(type1).build(),
                    MemberPersonality.builder().member(member).personalityType(type2).build()
            ));

            // when
            MemberResponse response = memberService.createProfile(memberId, request);

            // then
            assertThat(response.getNickname()).isEqualTo("새닉네임");
            assertThat(response.getPhone()).isEqualTo("010-1234-5678");
            assertThat(response.getAge()).isEqualTo(25);
            assertThat(response.getGender()).isEqualTo(Gender.MALE);
            assertThat(response.getPersonalityTypes()).hasSize(2);
            assertThat(response.getNicknameChangedAt()).isNotNull();

            then(memberPersonalityRepository).should().deleteByMember(any());
            then(memberPersonalityRepository).should().saveAll(any());
        }

        @Test
        @DisplayName("중복된 닉네임으로 생성하면 DUPLICATE_NICKNAME 예외가 발생한다")
        void createProfile_withDuplicateNickname_throwsException() {
            // given
            Long memberId = 1L;
            Member member = Member.builder()
                    .email("test@example.com")
                    .nickname("임시닉네임")
                    .build();

            MemberCreateRequest request = new MemberCreateRequest();
            request.setNickname("중복닉네임");

            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("중복닉네임")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.createProfile(memberId, request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(exception -> {
                        MemberException memberException = (MemberException) exception;
                        assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.DUPLICATE_NICKNAME);
                    });
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID로 생성하면 MEMBER_NOT_FOUND 예외가 발생한다")
        void createProfile_withNonExistentMember_throwsException() {
            // given
            Long memberId = 999L;
            MemberCreateRequest request = new MemberCreateRequest();
            request.setNickname("새닉네임");

            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.createProfile(memberId, request))
                    .isInstanceOf(MemberException.class)
                    .satisfies(exception -> {
                        MemberException memberException = (MemberException) exception;
                        assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("회원 검색")
    class SearchMembers {

        @Test
        @DisplayName("닉네임/연동닉네임으로 회원 검색 시 SliceResponse를 반환한다")
        void searchMembers_returnsSliceResponse() {
            Long me = 1L;
            PageRequest pageable = PageRequest.of(0, 20);

            Member meMember = Member.builder().email("me@example.com").nickname("나").build();
            ReflectionTestUtils.setField(meMember, "id", me);

            Member found = Member.builder()
                    .email("neighbor@example.com")
                    .nickname("이웃멍멍")
                    .build();
            ReflectionTestUtils.setField(found, "id", 2L);

            Slice<Member> result = new SliceImpl<>(List.of(found), pageable, false);

            given(memberRepository.findById(me)).willReturn(Optional.of(meMember));
            given(memberRepository.findByNicknameContainingIgnoreCaseOrLinkedNicknameContainingIgnoreCaseAndIdNot(
                    "이웃", "이웃", me, pageable
            )).willReturn(result);
            given(memberPersonalityRepository.findByMember(found)).willReturn(List.of());

            SliceResponse<MemberResponse> response = memberService.searchMembers(me, "이웃", pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getNickname()).isEqualTo("이웃멍멍");
        }
    }

    @Nested
    @DisplayName("내 프로필 조회/수정")
    class MyProfile {

        @Test
        @DisplayName("내 프로필 조회에 성공한다")
        void getMyProfile_success() {
            Member member = Member.builder()
                    .email("me@test.com")
                    .nickname("내닉네임")
                    .memberType(MemberType.PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(member, "id", 1L);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(memberPersonalityRepository.findByMember(member)).willReturn(List.of());

            MemberResponse response = memberService.getMyProfile(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getNickname()).isEqualTo("내닉네임");
        }

        @Test
        @DisplayName("내 프로필 수정에 성공한다")
        void updateMyProfile_success() {
            Member member = Member.builder()
                    .email("me@test.com")
                    .nickname("기존닉네임")
                    .memberType(MemberType.PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(member, "id", 1L);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("수정닉네임")).willReturn(false);
            given(memberPersonalityRepository.findByMember(member)).willReturn(List.of());

            MemberProfilePatchRequest request = new MemberProfilePatchRequest();
            request.setNickname("수정닉네임");
            request.setAge(30);

            MemberResponse response = memberService.updateMyProfile(1L, request);

            assertThat(response.getNickname()).isEqualTo("수정닉네임");
            assertThat(response.getAge()).isEqualTo(30);
        }

        @Test
        @DisplayName("타 회원 프로필 조회에 성공한다")
        void getMemberProfile_success() {
            Member member = Member.builder()
                    .email("other@test.com")
                    .nickname("타회원")
                    .memberType(MemberType.NON_PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(member, "id", 2L);
            given(memberRepository.findById(2L)).willReturn(Optional.of(member));
            given(memberPersonalityRepository.findByMember(member)).willReturn(List.of());

            MemberResponse response = memberService.getMemberProfile(2L);

            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getNickname()).isEqualTo("타회원");
        }
    }

    @Nested
    @DisplayName("팔로우 관계")
    class FollowRelation {

        @Test
        @DisplayName("팔로우에 성공하면 isFollowing=true를 반환한다")
        void follow_success() {
            Member me = Member.builder().email("me@test.com").nickname("me").build();
            ReflectionTestUtils.setField(me, "id", 1L);
            Member target = Member.builder().email("you@test.com").nickname("you").build();
            ReflectionTestUtils.setField(target, "id", 2L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(me));
            given(memberRepository.findById(2L)).willReturn(Optional.of(target));
            given(memberFollowRepository.findByFollowerIdAndFollowingId(1L, 2L)).willReturn(Optional.empty());

            FollowStatusResponse response = memberService.follow(1L, 2L);

            assertThat(response.isFollowing()).isTrue();
            then(memberFollowRepository).should().save(any(MemberFollow.class));
        }

        @Test
        @DisplayName("언팔로우에 성공하면 isFollowing=false를 반환한다")
        void unfollow_success() {
            MemberFollow follow = MemberFollow.builder().followerId(1L).followingId(2L).build();
            given(memberFollowRepository.findByFollowerIdAndFollowingId(1L, 2L)).willReturn(Optional.of(follow));

            FollowStatusResponse response = memberService.unfollow(1L, 2L);

            assertThat(response.isFollowing()).isFalse();
            then(memberFollowRepository).should().delete(follow);
        }

        @Test
        @DisplayName("자기 자신을 팔로우하면 예외가 발생한다")
        void follow_self_throwsException() {
            assertThatThrownBy(() -> memberService.follow(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("팔로워 목록 조회에 성공한다")
        void getFollowers_success() {
            Member me = Member.builder().email("me@test.com").nickname("나").build();
            ReflectionTestUtils.setField(me, "id", 1L);
            Member follower = Member.builder().email("follower@test.com").nickname("팔로워").build();
            ReflectionTestUtils.setField(follower, "id", 2L);

            MemberFollow follow = MemberFollow.builder()
                    .followerId(2L)
                    .followingId(1L)
                    .build();
            ReflectionTestUtils.setField(follow, "createdAt", LocalDateTime.now());

            PageRequest pageable = PageRequest.of(0, 20);
            Slice<MemberFollow> slice = new SliceImpl<>(List.of(follow), pageable, false);
            given(memberRepository.findById(1L)).willReturn(Optional.of(me));
            given(memberFollowRepository.findAllByFollowingIdOrderByCreatedAtDesc(1L, pageable)).willReturn(slice);
            given(memberRepository.findAllById(List.of(2L))).willReturn(List.of(follower));

            SliceResponse<MemberFollowResponse> response = memberService.getFollowers(1L, pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getNickname()).isEqualTo("팔로워");
        }

        @Test
        @DisplayName("존재하지 않는 회원의 팔로워 목록을 조회하면 예외가 발생한다")
        void getFollowers_memberNotFound_throwsException() {
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            PageRequest pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() -> memberService.getFollowers(999L, pageable))
                    .isInstanceOf(MemberException.class)
                    .satisfies(exception -> {
                        MemberException memberException = (MemberException) exception;
                        assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("팔로잉 목록 조회에 성공한다")
        void getFollowing_success() {
            Member me = Member.builder().email("me@test.com").nickname("나").build();
            ReflectionTestUtils.setField(me, "id", 1L);
            Member following = Member.builder().email("following@test.com").nickname("팔로잉").build();
            ReflectionTestUtils.setField(following, "id", 3L);

            MemberFollow follow = MemberFollow.builder()
                    .followerId(1L)
                    .followingId(3L)
                    .build();
            ReflectionTestUtils.setField(follow, "createdAt", LocalDateTime.now());

            PageRequest pageable = PageRequest.of(0, 20);
            Slice<MemberFollow> slice = new SliceImpl<>(List.of(follow), pageable, false);
            given(memberRepository.findById(1L)).willReturn(Optional.of(me));
            given(memberFollowRepository.findAllByFollowerIdOrderByCreatedAtDesc(1L, pageable)).willReturn(slice);
            given(memberRepository.findAllById(List.of(3L))).willReturn(List.of(following));

            SliceResponse<MemberFollowResponse> response = memberService.getFollowing(1L, pageable);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getNickname()).isEqualTo("팔로잉");
        }

        @Test
        @DisplayName("존재하지 않는 회원의 팔로잉 목록을 조회하면 예외가 발생한다")
        void getFollowing_memberNotFound_throwsException() {
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            PageRequest pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() -> memberService.getFollowing(999L, pageable))
                    .isInstanceOf(MemberException.class)
                    .satisfies(exception -> {
                        MemberException memberException = (MemberException) exception;
                        assertThat(memberException.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("팔로우 상태 조회 시 팔로우 중이면 true를 반환한다")
        void getFollowStatus_following_returnsTrue() {
            given(memberFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);

            FollowStatusResponse response = memberService.getFollowStatus(1L, 2L);

            assertThat(response.isFollowing()).isTrue();
        }

        @Test
        @DisplayName("팔로우 상태 조회 시 팔로우 중이 아니면 false를 반환한다")
        void getFollowStatus_notFollowing_returnsFalse() {
            given(memberFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(false);

            FollowStatusResponse response = memberService.getFollowStatus(1L, 2L);

            assertThat(response.isFollowing()).isFalse();
        }

        @Test
        @DisplayName("팔로워 목록이 비어있으면 빈 목록을 반환한다")
        void getFollowers_empty_returnsEmptyList() {
            Member me = Member.builder().email("me@test.com").nickname("나").build();
            ReflectionTestUtils.setField(me, "id", 1L);

            PageRequest pageable = PageRequest.of(0, 20);
            Slice<MemberFollow> emptySlice = new SliceImpl<>(List.of(), pageable, false);
            given(memberRepository.findById(1L)).willReturn(Optional.of(me));
            given(memberFollowRepository.findAllByFollowingIdOrderByCreatedAtDesc(1L, pageable)).willReturn(emptySlice);
            given(memberRepository.findAllById(List.of())).willReturn(List.of());

            SliceResponse<MemberFollowResponse> response = memberService.getFollowers(1L, pageable);

            assertThat(response.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("산책 통계")
    class WalkStats {

        @Test
        @DisplayName("산책 통계 조회 시 126일 포인트와 합계를 반환한다")
        void getWalkStats_success() {
            Member member = Member.builder().email("me@test.com").nickname("me").build();
            ReflectionTestUtils.setField(member, "id", 1L);
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            WalkDiaryDailyCountProjection projection1 = new WalkDiaryDailyCountProjection() {
                @Override
                public LocalDate getWalkDate() {
                    return LocalDate.now().minusDays(1);
                }

                @Override
                public long getWalkCount() {
                    return 2L;
                }
            };
            WalkDiaryDailyCountProjection projection2 = new WalkDiaryDailyCountProjection() {
                @Override
                public LocalDate getWalkDate() {
                    return LocalDate.now();
                }

                @Override
                public long getWalkCount() {
                    return 1L;
                }
            };
            given(walkDiaryRepository.countDailyWalks(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(projection1, projection2));

            var response = memberService.getWalkStats(1L);

            assertThat(response.getWindowDays()).isEqualTo(126);
            assertThat(response.getPoints()).hasSize(126);
            assertThat(response.getTotalWalks()).isEqualTo(3);
        }
    }
}
