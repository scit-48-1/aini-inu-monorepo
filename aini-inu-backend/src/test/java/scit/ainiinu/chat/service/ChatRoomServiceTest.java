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
import scit.ainiinu.chat.entity.ChatReview;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantPetRepository;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatReviewRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.entity.enums.PetGender;
import scit.ainiinu.pet.entity.enums.PetSize;
import scit.ainiinu.pet.repository.PetRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private ChatParticipantPetRepository chatParticipantPetRepository;

    @Mock
    private ChatReviewRepository chatReviewRepository;

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
            given(messageRepository.countUnreadByRoomIds(eq(memberId), anyList()))
                    .willReturn(Collections.emptyMap());

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
            given(messageRepository.countUnreadByRoomIds(eq(memberId), anyList()))
                    .willReturn(Collections.emptyMap());

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
            given(messageRepository.countUnreadByRoomIds(eq(memberId), anyList()))
                    .willReturn(Collections.emptyMap());

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
            given(messageRepository.countUnreadByRoomIds(eq(memberId), anyList()))
                    .willReturn(Collections.emptyMap());

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

        @Test
        @DisplayName("unreadCount가 응답에 포함된다")
        void unreadCount_includedInResponse() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom room1 = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room1, 10L);
            ChatRoom room2 = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room2, 20L);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(room1, room2), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(anyList()))
                    .willReturn(Collections.emptyMap());

            ChatParticipant cp1 = createParticipant(1L, 10L, memberId);
            ChatParticipant cp2 = createParticipant(2L, 10L, 2L);
            ChatParticipant cp3 = createParticipant(3L, 20L, memberId);
            ChatParticipant cp4 = createParticipant(4L, 20L, 3L);
            given(chatParticipantRepository.findAllByChatRoomIdIn(anyList()))
                    .willReturn(List.of(cp1, cp2, cp3, cp4));

            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(2L, "유저2"), createMember(3L, "유저3")));

            // room1: 5건 unread, room2: 0건
            given(messageRepository.countUnreadByRoomIds(eq(memberId), anyList()))
                    .willReturn(Map.of(10L, 5L));

            // when
            SliceResponse<ChatRoomSummaryResponse> result = chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            List<ChatRoomSummaryResponse> content = result.getContent();
            assertThat(content).hasSize(2);

            ChatRoomSummaryResponse summary1 = content.stream()
                    .filter(r -> r.getChatRoomId().equals(10L)).findFirst().orElseThrow();
            assertThat(summary1.getUnreadCount()).isEqualTo(5);

            ChatRoomSummaryResponse summary2 = content.stream()
                    .filter(r -> r.getChatRoomId().equals(20L)).findFirst().orElseThrow();
            assertThat(summary2.getUnreadCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("unread가 없는 방은 unreadCount가 0이다")
        void unreadCount_zeroWhenAllRead() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(room), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(anyList()))
                    .willReturn(Collections.emptyMap());

            ChatParticipant me = createParticipant(1L, 10L, memberId);
            ChatParticipant partner = createParticipant(2L, 10L, 2L);
            given(chatParticipantRepository.findAllByChatRoomIdIn(anyList()))
                    .willReturn(List.of(me, partner));
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(2L, "홍길동")));

            // 빈 맵 → unread 없음
            given(messageRepository.countUnreadByRoomIds(eq(memberId), anyList()))
                    .willReturn(Collections.emptyMap());

            // when
            SliceResponse<ChatRoomSummaryResponse> result = chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            assertThat(result.getContent().get(0).getUnreadCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("countUnreadByRoomIds가 호출된다")
        void countUnreadByRoomIds_called() {
            // given
            Long memberId = 1L;
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), any(), any(), eq(pageable)))
                    .willReturn(new SliceImpl<>(List.of(room), pageable, false));
            given(messageRepository.findLastMessagesByRoomIds(anyList()))
                    .willReturn(Collections.emptyMap());

            ChatParticipant me = createParticipant(1L, 10L, memberId);
            ChatParticipant partner = createParticipant(2L, 10L, 2L);
            given(chatParticipantRepository.findAllByChatRoomIdIn(anyList()))
                    .willReturn(List.of(me, partner));
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(2L, "홍길동")));
            given(messageRepository.countUnreadByRoomIds(eq(memberId), anyList()))
                    .willReturn(Collections.emptyMap());

            // when
            chatRoomService.getRooms(memberId, null, null, pageable);

            // then
            then(messageRepository).should().countUnreadByRoomIds(eq(memberId), eq(List.of(10L)));
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
        @DisplayName("WALK origin 채팅방 상세 조회 시 threadId와 roomTitle이 포함된다")
        void detailResponse_containsThreadIdAndRoomTitle() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            Long threadId = 42L;
            String roomTitle = "한강 산책 같이 해요!";

            ChatRoom room = ChatRoom.create(threadId, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, roomTitle);
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
            assertThat(response.getThreadId()).isEqualTo(threadId);
            assertThat(response.getRoomTitle()).isEqualTo(roomTitle);
            assertThat(response.getOrigin()).isEqualTo("WALK");
        }

        @Test
        @DisplayName("DM origin 채팅방 상세 조회 시 threadId와 roomTitle이 null이다")
        void detailResponse_dmRoom_threadIdAndRoomTitleAreNull() {
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
            assertThat(response.getThreadId()).isNull();
            assertThat(response.getRoomTitle()).isNull();
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

    @Nested
    @DisplayName("대시보드용 배치 조회")
    class DashboardBatchQueries {

        @Test
        @DisplayName("최근 친구와 리뷰 대기 목록을 배치 조회 결과로 조합한다")
        void returnsRecentFriendsAndPendingReviews() {
            Long memberId = 1L;

            ChatRoom recentRoom = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(recentRoom, 10L);

            ChatRoom walkRoom = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            setRoomId(walkRoom, 20L);
            ReflectionTestUtils.setField(walkRoom, "walkConfirmed", true);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), eq(null), eq(null), eq(PageRequest.of(0, 5))))
                    .willReturn(new SliceImpl<>(List.of(recentRoom), PageRequest.of(0, 5), false));
            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), eq(null), eq(ChatRoomOrigin.WALK), eq(PageRequest.of(0, 20))))
                    .willReturn(new SliceImpl<>(List.of(walkRoom), PageRequest.of(0, 20), false));

            ChatParticipant meRecent = createParticipant(1L, 10L, memberId);
            ChatParticipant partnerRecent = createParticipant(2L, 10L, 2L);
            ChatParticipant meWalk = createParticipant(3L, 20L, memberId);
            ChatParticipant partnerWalk = createParticipant(4L, 20L, 3L);

            given(chatParticipantRepository.findAllByChatRoomIdIn(List.of(10L)))
                    .willReturn(List.of(meRecent, partnerRecent));
            given(chatParticipantRepository.findAllByChatRoomIdIn(List.of(20L)))
                    .willReturn(List.of(meWalk, partnerWalk));

            Member me = createMember(1L, "나");
            Member recentPartnerMember = createMember(2L, "친구1");
            ReflectionTestUtils.setField(recentPartnerMember, "profileImageUrl", "https://example.com/recent.jpg");
            Member walkPartnerMember = createMember(3L, "친구2");
            ReflectionTestUtils.setField(walkPartnerMember, "profileImageUrl", "https://example.com/walk.jpg");
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(me, recentPartnerMember))
                    .willReturn(List.of(me, walkPartnerMember));

            ChatParticipantPet recentPartnerPet = ChatParticipantPet.of(partnerRecent.getId(), 100L);
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(recentPartnerPet));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(List.of(3L, 4L)))
                    .willReturn(List.of());

            Pet recentPet = createPet(100L, "몽이");
            given(petRepository.findAllById(List.of(100L))).willReturn(List.of(recentPet));

            given(chatReviewRepository.findByReviewerIdAndChatRoomIdIn(memberId, List.of(20L)))
                    .willReturn(List.of());

            var recentFriends = chatRoomService.getDashboardRecentFriends(memberId, 5);
            var pendingReviews = chatRoomService.getDashboardPendingReviews(memberId, 20);

            assertThat(recentFriends).hasSize(1);
            assertThat(recentFriends.get(0).getMemberId()).isEqualTo(2L);
            assertThat(recentFriends.get(0).getDisplayName()).isEqualTo("몽이");
            assertThat(recentFriends.get(0).getRoomId()).isEqualTo(10L);

            assertThat(pendingReviews).hasSize(1);
            assertThat(pendingReviews.get(0).getChatRoomId()).isEqualTo(20L);
            assertThat(pendingReviews.get(0).getPartnerId()).isEqualTo(3L);
            assertThat(pendingReviews.get(0).getPartnerNickname()).isEqualTo("친구2");
        }

        @Test
        @DisplayName("이미 리뷰한 WALK 방은 리뷰 대기 목록에서 제외한다")
        void excludesReviewedRoomsFromPendingReviews() {
            Long memberId = 1L;

            ChatRoom walkRoom = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            setRoomId(walkRoom, 20L);
            ReflectionTestUtils.setField(walkRoom, "walkConfirmed", true);

            given(chatRoomRepository.findAccessibleRoomsByMemberId(eq(memberId), eq(null), eq(ChatRoomOrigin.WALK), eq(PageRequest.of(0, 20))))
                    .willReturn(new SliceImpl<>(List.of(walkRoom), PageRequest.of(0, 20), false));

            ChatParticipant meWalk = createParticipant(3L, 20L, memberId);
            ChatParticipant partnerWalk = createParticipant(4L, 20L, 3L);
            given(chatParticipantRepository.findAllByChatRoomIdIn(List.of(20L)))
                    .willReturn(List.of(meWalk, partnerWalk));
            given(memberRepository.findAllById(any()))
                    .willReturn(List.of(createMember(1L, "나"), createMember(3L, "친구2")));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(List.of(3L, 4L)))
                    .willReturn(List.of());
            given(chatReviewRepository.findByReviewerIdAndChatRoomIdIn(memberId, List.of(20L)))
                    .willReturn(List.of(ChatReview.create(20L, memberId, 3L, 5, "good")));

            var result = chatRoomService.getDashboardPendingReviews(memberId, 20);

            assertThat(result).isEmpty();
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

    private Pet createPet(Long id, String name) {
        Pet pet = Pet.builder()
                .memberId(1L)
                .name(name)
                .age(2)
                .gender(PetGender.MALE)
                .size(PetSize.MEDIUM)
                .isNeutered(true)
                .isMain(true)
                .build();
        ReflectionTestUtils.setField(pet, "id", id);
        return pet;
    }
}
