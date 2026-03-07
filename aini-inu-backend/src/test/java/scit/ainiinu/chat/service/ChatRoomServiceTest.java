package scit.ainiinu.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.chat.dto.response.ChatParticipantResponse;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.dto.response.ChatRoomSummaryResponse;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatParticipantPet;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantPetRepository;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.repository.PetRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private ChatParticipantPetRepository chatParticipantPetRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Nested
    @DisplayName("채팅방 목록 조회 (getRooms)")
    class GetRooms {

        @Test
        @DisplayName("1:1 채팅방 목록에서 상대방 닉네임이 displayName으로 반환된다")
        void returnsPartnerNicknameAsDisplayName() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(room), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(List.of(10L)))
                    .willReturn(Collections.emptyMap());

            ChatParticipant me = createParticipant(1L, 10L, memberId);
            ChatParticipant partner = createParticipant(2L, 10L, 2L);
            given(chatParticipantRepository.findAllByChatRoomIdIn(List.of(10L)))
                    .willReturn(List.of(me, partner));

            Member partnerMember = createMember(2L, "홍길동");
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(partnerMember));

            // when
            SliceResponse<ChatRoomSummaryResponse> result = chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDisplayName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("그룹 채팅방에서 상대방 2명의 닉네임이 쉼표로 구분되어 displayName으로 반환된다")
        void returnsCommaSeparatedNicknamesForGroupRoom() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(room), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(List.of(10L)))
                    .willReturn(Collections.emptyMap());

            ChatParticipant me = createParticipant(1L, 10L, memberId);
            ChatParticipant partner1 = createParticipant(2L, 10L, 2L);
            ChatParticipant partner2 = createParticipant(3L, 10L, 3L);
            given(chatParticipantRepository.findAllByChatRoomIdIn(List.of(10L)))
                    .willReturn(List.of(me, partner1, partner2));

            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(2L, "홍길동"), createMember(3L, "김철수")));

            // when
            SliceResponse<ChatRoomSummaryResponse> result = chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            assertThat(result.getContent().get(0).getDisplayName()).isEqualTo("홍길동, 김철수");
        }

        @Test
        @DisplayName("상대방이 3명 이상이면 displayName에 '외 N명' 형식으로 반환된다")
        void returnsEtcFormatWhenMoreThanTwoPartners() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(room), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(List.of(10L)))
                    .willReturn(Collections.emptyMap());

            ChatParticipant me = createParticipant(1L, 10L, memberId);
            ChatParticipant partner1 = createParticipant(2L, 10L, 2L);
            ChatParticipant partner2 = createParticipant(3L, 10L, 3L);
            ChatParticipant partner3 = createParticipant(4L, 10L, 4L);
            given(chatParticipantRepository.findAllByChatRoomIdIn(List.of(10L)))
                    .willReturn(List.of(me, partner1, partner2, partner3));

            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(
                            createMember(2L, "홍길동"),
                            createMember(3L, "김철수"),
                            createMember(4L, "이영희")
                    ));

            // when
            SliceResponse<ChatRoomSummaryResponse> result = chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            assertThat(result.getContent().get(0).getDisplayName()).isEqualTo("홍길동, 김철수 외 1명");
        }

        @Test
        @DisplayName("상대방이 모두 나간 채팅방이면 displayName이 '알 수 없음'으로 반환된다")
        void returnsUnknownWhenAllPartnersLeft() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(room), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(List.of(10L)))
                    .willReturn(Collections.emptyMap());

            ChatParticipant me = createParticipant(1L, 10L, memberId);
            ChatParticipant partner = createParticipant(2L, 10L, 2L);
            partner.leave(); // 상대방이 나감
            given(chatParticipantRepository.findAllByChatRoomIdIn(List.of(10L)))
                    .willReturn(List.of(me, partner));

            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(2L, "홍길동")));

            // when
            SliceResponse<ChatRoomSummaryResponse> result = chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            assertThat(result.getContent().get(0).getDisplayName()).isEqualTo("알 수 없음");
        }

        @Test
        @DisplayName("채팅방이 없으면 빈 목록을 반환한다")
        void returnsEmptyListWhenNoRooms() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(List.of()))
                    .willReturn(Collections.emptyMap());

            // when
            SliceResponse<ChatRoomSummaryResponse> result = chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("채팅방 상세 조회 (getRoomDetail)")
    class GetRoomDetail {

        @Test
        @DisplayName("참여자 응답에 회원 닉네임이 포함된다")
        void participantResponseIncludesNickname() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, chatRoomId);

            ChatParticipant me = createParticipant(1L, chatRoomId, memberId);
            ChatParticipant partner = createParticipant(2L, chatRoomId, 2L);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(true);
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findAllByChatRoomId(chatRoomId))
                    .willReturn(List.of(me, partner));
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(1L, "나"), createMember(2L, "홍길동")));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(any()))
                    .willReturn(Collections.emptyList());
            given(messageRepository.findTopByChatRoomIdOrderByIdDesc(chatRoomId))
                    .willReturn(Optional.empty());

            // when
            ChatRoomDetailResponse response = chatRoomService.getRoomDetail(memberId, chatRoomId);

            // then
            assertThat(response.getParticipants()).hasSize(2);

            ChatParticipantResponse myResponse = response.getParticipants().stream()
                    .filter(p -> p.getMemberId().equals(memberId))
                    .findFirst().orElseThrow();
            assertThat(myResponse.getNickname()).isEqualTo("나");

            ChatParticipantResponse partnerResponse = response.getParticipants().stream()
                    .filter(p -> p.getMemberId().equals(2L))
                    .findFirst().orElseThrow();
            assertThat(partnerResponse.getNickname()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("회원 정보가 없는 참여자는 닉네임이 null이다")
        void participantWithoutMemberInfoHasNullNickname() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, chatRoomId);

            ChatParticipant me = createParticipant(1L, chatRoomId, memberId);
            ChatParticipant partner = createParticipant(2L, chatRoomId, 2L);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(true);
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findAllByChatRoomId(chatRoomId))
                    .willReturn(List.of(me, partner));
            // memberId=2의 Member가 조회되지 않는 경우
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(1L, "나")));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(any()))
                    .willReturn(Collections.emptyList());
            given(messageRepository.findTopByChatRoomIdOrderByIdDesc(chatRoomId))
                    .willReturn(Optional.empty());

            // when
            ChatRoomDetailResponse response = chatRoomService.getRoomDetail(memberId, chatRoomId);

            // then
            ChatParticipantResponse partnerResponse = response.getParticipants().stream()
                    .filter(p -> p.getMemberId().equals(2L))
                    .findFirst().orElseThrow();
            assertThat(partnerResponse.getNickname()).isNull();
        }

        @Test
        @DisplayName("DM origin 채팅방을 조회하면 origin이 DM으로 반환된다")
        void detailResponse_containsDmOrigin() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, chatRoomId);

            ChatParticipant me = createParticipant(1L, chatRoomId, memberId);
            ChatParticipant partner = createParticipant(2L, chatRoomId, 2L);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(true);
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findAllByChatRoomId(chatRoomId))
                    .willReturn(List.of(me, partner));
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(1L, "나"), createMember(2L, "홍길동")));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(any()))
                    .willReturn(Collections.emptyList());
            given(messageRepository.findTopByChatRoomIdOrderByIdDesc(chatRoomId))
                    .willReturn(Optional.empty());

            // when
            ChatRoomDetailResponse response = chatRoomService.getRoomDetail(memberId, chatRoomId);

            // then
            assertThat(response.getOrigin()).isEqualTo("DM");
        }

        @Test
        @DisplayName("WALK origin 채팅방을 조회하면 origin이 WALK으로 반환된다")
        void detailResponse_containsWalkOrigin() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            setRoomId(room, chatRoomId);

            ChatParticipant me = createParticipant(1L, chatRoomId, memberId);
            ChatParticipant partner = createParticipant(2L, chatRoomId, 2L);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(true);
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findAllByChatRoomId(chatRoomId))
                    .willReturn(List.of(me, partner));
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(1L, "나"), createMember(2L, "홍길동")));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(any()))
                    .willReturn(Collections.emptyList());
            given(messageRepository.findTopByChatRoomIdOrderByIdDesc(chatRoomId))
                    .willReturn(Optional.empty());

            // when
            ChatRoomDetailResponse response = chatRoomService.getRoomDetail(memberId, chatRoomId);

            // then
            assertThat(response.getOrigin()).isEqualTo("WALK");
        }

        @Test
        @DisplayName("LOST_PET origin 채팅방을 조회하면 origin이 LOST_PET으로 반환된다")
        void detailResponse_containsLostPetOrigin() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.LOST_PET, null);
            setRoomId(room, chatRoomId);

            ChatParticipant me = createParticipant(1L, chatRoomId, memberId);
            ChatParticipant partner = createParticipant(2L, chatRoomId, 2L);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(true);
            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findAllByChatRoomId(chatRoomId))
                    .willReturn(List.of(me, partner));
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(1L, "나"), createMember(2L, "홍길동")));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(any()))
                    .willReturn(Collections.emptyList());
            given(messageRepository.findTopByChatRoomIdOrderByIdDesc(chatRoomId))
                    .willReturn(Optional.empty());

            // when
            ChatRoomDetailResponse response = chatRoomService.getRoomDetail(memberId, chatRoomId);

            // then
            assertThat(response.getOrigin()).isEqualTo("LOST_PET");
        }

        @Test
        @DisplayName("참여자가 아닌 회원이 조회하면 ROOM_ACCESS_DENIED 예외가 발생한다")
        void nonParticipant_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> chatRoomService.getRoomDetail(memberId, chatRoomId))
                    .isInstanceOf(ChatException.class);
        }
    }

    // --- 헬퍼 메서드 ---

    private void setRoomId(ChatRoom room, Long id) {
        ReflectionTestUtils.setField(room, "id", id);
    }

    private ChatParticipant createParticipant(Long participantId, Long chatRoomId, Long memberId) {
        ChatParticipant participant = ChatParticipant.create(chatRoomId, memberId);
        ReflectionTestUtils.setField(participant, "id", participantId);
        return participant;
    }

    private Member createMember(Long id, String nickname) {
        Member member = Member.builder()
                .email(nickname + "@test.com")
                .nickname(nickname)
                .memberType(MemberType.PET_OWNER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
