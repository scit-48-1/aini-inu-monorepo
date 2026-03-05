package scit.ainiinu.lostpet.domain;

import java.math.BigDecimal;
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
@Table(name = "lost_pet_search_candidate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostPetSearchCandidate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LostPetSearchSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sighting_id", nullable = false)
    private Sighting sighting;

    @Column(name = "score_similarity", nullable = false, precision = 6, scale = 5)
    private BigDecimal scoreSimilarity;

    @Column(name = "score_distance", nullable = false, precision = 6, scale = 5)
    private BigDecimal scoreDistance;

    @Column(name = "score_recency", nullable = false, precision = 6, scale = 5)
    private BigDecimal scoreRecency;

    @Column(name = "score_total", nullable = false, precision = 6, scale = 5)
    private BigDecimal scoreTotal;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LostPetSearchCandidateStatus status;

    private LostPetSearchCandidate(
            LostPetSearchSession session,
            Sighting sighting,
            BigDecimal scoreSimilarity,
            BigDecimal scoreDistance,
            BigDecimal scoreRecency,
            BigDecimal scoreTotal,
            Integer rankOrder
    ) {
        this.session = session;
        this.sighting = sighting;
        this.scoreSimilarity = scoreSimilarity;
        this.scoreDistance = scoreDistance;
        this.scoreRecency = scoreRecency;
        this.scoreTotal = scoreTotal;
        this.rankOrder = rankOrder;
        this.status = LostPetSearchCandidateStatus.CANDIDATE;
    }

    public static LostPetSearchCandidate create(
            LostPetSearchSession session,
            Sighting sighting,
            BigDecimal scoreSimilarity,
            BigDecimal scoreDistance,
            BigDecimal scoreRecency,
            BigDecimal scoreTotal,
            Integer rankOrder
    ) {
        return new LostPetSearchCandidate(
                session,
                sighting,
                scoreSimilarity,
                scoreDistance,
                scoreRecency,
                scoreTotal,
                rankOrder
        );
    }

    public void approve() {
        this.status = LostPetSearchCandidateStatus.APPROVED;
    }
}
