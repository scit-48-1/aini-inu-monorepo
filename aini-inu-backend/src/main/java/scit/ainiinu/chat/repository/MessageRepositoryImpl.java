package scit.ainiinu.chat.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import scit.ainiinu.chat.entity.Message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<Message> findByRoomIdWithCursor(Long chatRoomId, Long cursor, int size, String direction) {
        String jpql = """
                select m
                from Message m
                where m.chatRoomId = :chatRoomId
                  and (:cursor is null or m.id < :cursor)
                order by m.id desc
                """;

        // currently only backward cursor pagination is supported by contract.
        if (!"before".equals(direction)) {
            throw new IllegalArgumentException("Unsupported direction: " + direction);
        }

        TypedQuery<Message> query = entityManager.createQuery(jpql, Message.class)
                .setParameter("chatRoomId", chatRoomId)
                .setParameter("cursor", cursor)
                .setMaxResults(size);

        return query.getResultList();
    }

    @Override
    public Map<Long, Message> findLastMessagesByRoomIds(List<Long> chatRoomIds) {
        if (chatRoomIds == null || chatRoomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Subquery: max message ID per room → single query instead of N
        String jpql = """
                select m
                from Message m
                where m.id in (
                    select max(m2.id)
                    from Message m2
                    where m2.chatRoomId in :roomIds
                    group by m2.chatRoomId
                )
                """;

        List<Message> lastMessages = entityManager.createQuery(jpql, Message.class)
                .setParameter("roomIds", chatRoomIds)
                .getResultList();

        Map<Long, Message> result = new LinkedHashMap<>();
        for (Message m : lastMessages) {
            result.put(m.getChatRoomId(), m);
        }
        return result;
    }

    @Override
    public Map<Long, Long> countUnreadByRoomIds(Long memberId, List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = """
                select cp.chatRoomId, count(m)
                from Message m, ChatParticipant cp
                where m.chatRoomId = cp.chatRoomId
                  and cp.memberId = :memberId
                  and cp.chatRoomId in :roomIds
                  and cp.leftAt is null
                  and (cp.lastReadMessageId is null or m.id > cp.lastReadMessageId)
                group by cp.chatRoomId
                """;

        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class)
                .setParameter("memberId", memberId)
                .setParameter("roomIds", roomIds)
                .getResultList();

        Map<Long, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }
}
