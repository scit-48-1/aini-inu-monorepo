package scit.ainiinu.chat.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import scit.ainiinu.chat.entity.ChatMessageType;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.common.config.JpaConfig;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:chat-unread-sort;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class ChatUnreadAndSortRepositorySliceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private MessageRepository messageRepository;

    // ---- helpers ----

    private ChatRoom createRoom() {
        return chatRoomRepository.save(
                ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null));
    }

    private ChatParticipant addParticipant(Long roomId, Long memberId) {
        return chatParticipantRepository.save(ChatParticipant.create(roomId, memberId));
    }

    private Message sendMessage(Long roomId, Long senderId, String content) {
        return messageRepository.save(
                Message.create(roomId, senderId, content, ChatMessageType.USER, null));
    }

    // ================================================================
    //  countUnreadByRoomIds
    // ================================================================

    @Nested
    @DisplayName("countUnreadByRoomIds")
    class CountUnread {

        @Test
        @DisplayName("lastReadMessageId가 null이면 모든 메시지가 unread로 카운트된다")
        void allUnread_whenLastReadIsNull() {
            // given
            ChatRoom room = createRoom();
            ChatParticipant cp = addParticipant(room.getId(), 1L);
            addParticipant(room.getId(), 2L);

            sendMessage(room.getId(), 2L, "m1");
            sendMessage(room.getId(), 2L, "m2");
            sendMessage(room.getId(), 2L, "m3");
            messageRepository.flush();

            // when
            Map<Long, Long> result = messageRepository.countUnreadByRoomIds(1L, List.of(room.getId()));

            // then
            assertThat(result).containsEntry(room.getId(), 3L);
        }

        @Test
        @DisplayName("lastReadMessageId 이후 메시지만 unread로 카운트된다")
        void partialUnread_whenSomeRead() {
            // given
            ChatRoom room = createRoom();
            ChatParticipant cp = addParticipant(room.getId(), 1L);
            addParticipant(room.getId(), 2L);

            Message m1 = sendMessage(room.getId(), 2L, "m1");
            Message m2 = sendMessage(room.getId(), 2L, "m2");
            Message m3 = sendMessage(room.getId(), 2L, "m3");
            messageRepository.flush();

            // member1이 m1까지 읽음
            cp.markRead(m1.getId());
            chatParticipantRepository.flush();

            // when
            Map<Long, Long> result = messageRepository.countUnreadByRoomIds(1L, List.of(room.getId()));

            // then
            assertThat(result).containsEntry(room.getId(), 2L);
        }

        @Test
        @DisplayName("모든 메시지를 읽으면 결과에 해당 방이 포함되지 않는다")
        void zeroUnread_whenAllRead() {
            // given
            ChatRoom room = createRoom();
            ChatParticipant cp = addParticipant(room.getId(), 1L);
            addParticipant(room.getId(), 2L);

            Message m1 = sendMessage(room.getId(), 2L, "m1");
            messageRepository.flush();

            cp.markRead(m1.getId());
            chatParticipantRepository.flush();

            // when
            Map<Long, Long> result = messageRepository.countUnreadByRoomIds(1L, List.of(room.getId()));

            // then
            assertThat(result).doesNotContainKey(room.getId());
        }

        @Test
        @DisplayName("여러 방의 unread count를 배치로 조회한다")
        void batchCount_multipleRooms() {
            // given
            ChatRoom room1 = createRoom();
            ChatRoom room2 = createRoom();

            addParticipant(room1.getId(), 1L);
            addParticipant(room1.getId(), 2L);
            addParticipant(room2.getId(), 1L);
            addParticipant(room2.getId(), 3L);

            sendMessage(room1.getId(), 2L, "r1-m1");
            sendMessage(room1.getId(), 2L, "r1-m2");
            sendMessage(room2.getId(), 3L, "r2-m1");
            messageRepository.flush();

            // when
            Map<Long, Long> result = messageRepository.countUnreadByRoomIds(
                    1L, List.of(room1.getId(), room2.getId()));

            // then
            assertThat(result).containsEntry(room1.getId(), 2L);
            assertThat(result).containsEntry(room2.getId(), 1L);
        }

        @Test
        @DisplayName("빈 roomIds 리스트는 빈 맵을 반환한다")
        void emptyRoomIds_returnsEmptyMap() {
            Map<Long, Long> result = messageRepository.countUnreadByRoomIds(1L, List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("탈퇴한 참여자(leftAt != null)는 unread 카운트에서 제외된다")
        void leftParticipant_excluded() {
            // given
            ChatRoom room = createRoom();
            ChatParticipant cp = addParticipant(room.getId(), 1L);
            addParticipant(room.getId(), 2L);

            sendMessage(room.getId(), 2L, "m1");
            messageRepository.flush();

            cp.leave();
            chatParticipantRepository.flush();

            // when
            Map<Long, Long> result = messageRepository.countUnreadByRoomIds(1L, List.of(room.getId()));

            // then
            assertThat(result).doesNotContainKey(room.getId());
        }

        @Test
        @DisplayName("본인이 보낸 메시지도 unread로 카운트된다")
        void ownMessages_countedAsUnread() {
            // given
            ChatRoom room = createRoom();
            addParticipant(room.getId(), 1L);
            addParticipant(room.getId(), 2L);

            sendMessage(room.getId(), 1L, "내가 보낸 메시지");
            sendMessage(room.getId(), 2L, "상대가 보낸 메시지");
            messageRepository.flush();

            // when
            Map<Long, Long> result = messageRepository.countUnreadByRoomIds(1L, List.of(room.getId()));

            // then — lastReadMessageId가 null이므로 모든 메시지가 unread
            assertThat(result).containsEntry(room.getId(), 2L);
        }
    }

    // ================================================================
    //  updateLastMessageAt
    // ================================================================

    @Nested
    @DisplayName("updateLastMessageAt")
    class UpdateLastMessageAt {

        @Test
        @DisplayName("lastMessageAt이 null일 때 업데이트된다")
        void updatesWhenNull() {
            // given
            ChatRoom room = createRoom();
            chatRoomRepository.flush();
            OffsetDateTime now = OffsetDateTime.now();

            // when
            chatRoomRepository.updateLastMessageAt(room.getId(), now);
            chatRoomRepository.flush();
            entityManager.clear(); // @Modifying 쿼리 후 캐시 무효화

            // then
            ChatRoom updated = chatRoomRepository.findById(room.getId()).orElseThrow();
            assertThat(updated.getLastMessageAt()).isNotNull();
        }

        @Test
        @DisplayName("더 최신 시각으로만 업데이트된다 (가드 조건)")
        void onlyUpdatesWithNewerTimestamp() {
            // given
            ChatRoom room = createRoom();
            chatRoomRepository.flush();

            OffsetDateTime t1 = OffsetDateTime.now();
            OffsetDateTime t2 = t1.plusMinutes(5);
            OffsetDateTime t3 = t1.minusMinutes(5); // 더 과거

            chatRoomRepository.updateLastMessageAt(room.getId(), t2);
            chatRoomRepository.flush();
            entityManager.clear();

            // when — 과거 시각으로 업데이트 시도
            chatRoomRepository.updateLastMessageAt(room.getId(), t3);
            chatRoomRepository.flush();
            entityManager.clear();

            // then — t2가 유지됨
            ChatRoom updated = chatRoomRepository.findById(room.getId()).orElseThrow();
            assertThat(updated.getLastMessageAt()).isEqualToIgnoringNanos(t2);
        }
    }

    // ================================================================
    //  채팅방 정렬: lastMessageAt DESC NULLS LAST
    // ================================================================

    @Nested
    @DisplayName("채팅방 목록 정렬")
    class RoomSorting {

        @Test
        @DisplayName("lastMessageAt이 최신인 방이 먼저 반환된다")
        void orderedByLastMessageAtDesc() {
            // given
            ChatRoom room1 = createRoom();
            ChatRoom room2 = createRoom();
            ChatRoom room3 = createRoom();

            addParticipant(room1.getId(), 1L);
            addParticipant(room2.getId(), 1L);
            addParticipant(room3.getId(), 1L);

            OffsetDateTime now = OffsetDateTime.now();
            chatRoomRepository.flush();

            // room2가 가장 최신, room1이 중간, room3이 가장 오래됨
            chatRoomRepository.updateLastMessageAt(room3.getId(), now.minusHours(2));
            chatRoomRepository.updateLastMessageAt(room1.getId(), now.minusHours(1));
            chatRoomRepository.updateLastMessageAt(room2.getId(), now);
            chatRoomRepository.flush();
            entityManager.clear();

            // when
            Slice<ChatRoom> result = chatRoomRepository.findAccessibleRoomsByMemberId(
                    1L, null, null, PageRequest.of(0, 10));

            // then
            List<Long> ids = result.getContent().stream().map(ChatRoom::getId).toList();
            assertThat(ids).containsExactly(room2.getId(), room1.getId(), room3.getId());
        }

        @Test
        @DisplayName("lastMessageAt이 null인 방은 뒤로 밀린다 (NULLS LAST)")
        void nullLastMessageAt_sortedLast() {
            // given
            ChatRoom roomWithMessage = createRoom();
            ChatRoom roomWithoutMessage = createRoom();

            addParticipant(roomWithMessage.getId(), 1L);
            addParticipant(roomWithoutMessage.getId(), 1L);
            chatRoomRepository.flush();

            chatRoomRepository.updateLastMessageAt(roomWithMessage.getId(), OffsetDateTime.now());
            chatRoomRepository.flush();
            entityManager.clear();

            // when
            Slice<ChatRoom> result = chatRoomRepository.findAccessibleRoomsByMemberId(
                    1L, null, null, PageRequest.of(0, 10));

            // then
            List<Long> ids = result.getContent().stream().map(ChatRoom::getId).toList();
            assertThat(ids.get(0)).isEqualTo(roomWithMessage.getId());
            assertThat(ids.get(1)).isEqualTo(roomWithoutMessage.getId());
        }
    }
}
