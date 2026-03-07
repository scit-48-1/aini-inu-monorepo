package scit.ainiinu.chat.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import scit.ainiinu.chat.entity.ChatMessageType;
import scit.ainiinu.chat.entity.ChatReview;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.common.config.JpaConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:chat-repo-slice;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class ChatRepositorySliceTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatReviewRepository chatReviewRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    @DisplayName("chat_review는 (room, reviewer, reviewee) unique 제약을 가진다")
    void chatReview_uniqueConstraint() {
        // given
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null));
        chatReviewRepository.save(ChatReview.create(room.getId(), 1L, 2L, 5, "first"));

        // when & then
        assertThatThrownBy(() -> {
            chatReviewRepository.saveAndFlush(ChatReview.create(room.getId(), 1L, 2L, 4, "duplicate"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("메시지 커서 조회는 id < cursor 조건으로 최신순(id DESC) 반환한다")
    void messageCursorQuery_descAndLessThanCursor() {
        // given
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null));

        Message m1 = messageRepository.save(Message.create(room.getId(), 1L, "m1", ChatMessageType.USER, "c1"));
        Message m2 = messageRepository.save(Message.create(room.getId(), 1L, "m2", ChatMessageType.USER, "c2"));
        Message m3 = messageRepository.save(Message.create(room.getId(), 1L, "m3", ChatMessageType.USER, "c3"));
        messageRepository.flush();

        // when
        List<Message> messages = messageRepository.findByRoomIdWithCursor(room.getId(), m3.getId(), 10, "before");

        // then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getId()).isEqualTo(m2.getId());
        assertThat(messages.get(1).getId()).isEqualTo(m1.getId());
    }
}
