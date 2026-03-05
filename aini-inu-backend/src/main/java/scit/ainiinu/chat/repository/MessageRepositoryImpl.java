package scit.ainiinu.chat.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import scit.ainiinu.chat.entity.Message;

import java.util.List;

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
}
