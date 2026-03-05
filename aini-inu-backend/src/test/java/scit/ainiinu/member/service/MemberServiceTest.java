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
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.dto.request.MemberCreateRequest;
import scit.ainiinu.member.dto.response.MemberResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.MemberPersonality;
import scit.ainiinu.member.entity.MemberPersonalityType;
import scit.ainiinu.member.entity.enums.Gender;
import scit.ainiinu.member.exception.MemberErrorCode;
import scit.ainiinu.member.exception.MemberException;
import scit.ainiinu.member.repository.MemberFollowRepository;
import scit.ainiinu.member.repository.MemberPersonalityRepository;
import scit.ainiinu.member.repository.MemberPersonalityTypeRepository;
import scit.ainiinu.member.repository.MemberRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
