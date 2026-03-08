package scit.ainiinu.chat.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cr from ChatRoom cr where cr.id = :chatRoomId")
    Optional<ChatRoom> findByIdForUpdate(@Param("chatRoomId") Long chatRoomId);

    @Query("""
            select cr
            from ChatRoom cr
            where cr.chatType = :chatType
              and cr.status = :status
              and exists (
                    select 1 from ChatParticipant cp1
                    where cp1.chatRoomId = cr.id
                      and cp1.memberId = :memberA
                      and cp1.leftAt is null
              )
              and exists (
                    select 1 from ChatParticipant cp2
                    where cp2.chatRoomId = cr.id
                      and cp2.memberId = :memberB
                      and cp2.leftAt is null
              )
              and (
                    select count(cp3)
                    from ChatParticipant cp3
                    where cp3.chatRoomId = cr.id
                      and cp3.leftAt is null
              ) = 2
            """)
    Optional<ChatRoom> findByTypeAndParticipants(
            @Param("chatType") ChatRoomType chatType,
            @Param("status") ChatRoomStatus status,
            @Param("memberA") Long memberA,
            @Param("memberB") Long memberB
    );

    Optional<ChatRoom> findFirstByThreadIdAndChatTypeAndStatusOrderByIdAsc(
            Long threadId,
            ChatRoomType chatType,
            ChatRoomStatus status
    );

    @Query("""
            select cr
            from ChatRoom cr
            where cr.threadId = :threadId
              and cr.chatType = :chatType
              and cr.status = :status
              and exists (
                    select 1 from ChatParticipant cp1
                    where cp1.chatRoomId = cr.id
                      and cp1.memberId = :memberA
                      and cp1.leftAt is null
              )
              and exists (
                    select 1 from ChatParticipant cp2
                    where cp2.chatRoomId = cr.id
                      and cp2.memberId = :memberB
                      and cp2.leftAt is null
              )
              and (
                    select count(cp3)
                    from ChatParticipant cp3
                    where cp3.chatRoomId = cr.id
                      and cp3.leftAt is null
              ) = 2
            """)
    Optional<ChatRoom> findByThreadIdAndTypeAndParticipants(
            @Param("threadId") Long threadId,
            @Param("chatType") ChatRoomType chatType,
            @Param("status") ChatRoomStatus status,
            @Param("memberA") Long memberA,
            @Param("memberB") Long memberB
    );

    @Query("""
            select cr
            from ChatRoom cr
            where cr.id in (
                select cp.chatRoomId
                from ChatParticipant cp
                where cp.memberId = :memberId
                  and cp.leftAt is null
            )
              and (:status is null or cr.status = :status)
              and (:origin is null or cr.origin = :origin)
            order by cr.lastMessageAt desc nulls last, cr.id desc
            """)
    Slice<ChatRoom> findAccessibleRoomsByMemberId(
            @Param("memberId") Long memberId,
            @Param("status") ChatRoomStatus status,
            @Param("origin") ChatRoomOrigin origin,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE ChatRoom cr SET cr.lastMessageAt = :sentAt WHERE cr.id = :roomId AND (cr.lastMessageAt IS NULL OR cr.lastMessageAt < :sentAt)")
    void updateLastMessageAt(@Param("roomId") Long roomId, @Param("sentAt") OffsetDateTime sentAt);
}
