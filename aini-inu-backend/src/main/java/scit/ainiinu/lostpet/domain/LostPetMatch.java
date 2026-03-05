package scit.ainiinu.lostpet.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "lost_pet_match")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostPetMatch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_pet_id", nullable = false)
    private LostPetReport lostPetReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sighting_id", nullable = false)
    private Sighting sighting;

    @Column(name = "similarity_total", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private LostPetMatchStatus status;

    @Column(name = "approved_by_member_id")
    private Long approvedByMemberId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "invalidated_reason", length = 40)
    private LostPetMatchInvalidatedReason invalidatedReason;

    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;

    @Column(name = "matched_at", nullable = false)
    private LocalDateTime matchedAt;

    private LostPetMatch(LostPetReport lostPetReport, Sighting sighting, BigDecimal similarityTotal) {
        this.lostPetReport = lostPetReport;
        this.sighting = sighting;
        this.similarityTotal = similarityTotal;
        this.status = LostPetMatchStatus.PENDING_APPROVAL;
        this.matchedAt = LocalDateTime.now();
    }

    public static LostPetMatch create(LostPetReport lostPetReport, Sighting sighting, BigDecimal similarityTotal) {
        return new LostPetMatch(lostPetReport, sighting, similarityTotal);
    }

    public void approve(Long memberId) {
        this.status = LostPetMatchStatus.APPROVED;
        this.approvedByMemberId = memberId;
        this.approvedAt = LocalDateTime.now();
    }

    public void linkChatRoom(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
        this.status = LostPetMatchStatus.CHAT_LINKED;
    }

    public void markPendingChatLink() {
        this.status = LostPetMatchStatus.PENDING_CHAT_LINK;
        this.chatRoomId = null;
    }

    public void invalidate(LostPetMatchInvalidatedReason reason) {
        this.status = LostPetMatchStatus.INVALIDATED;
        this.invalidatedReason = reason;
        this.invalidatedAt = LocalDateTime.now();
    }
}
