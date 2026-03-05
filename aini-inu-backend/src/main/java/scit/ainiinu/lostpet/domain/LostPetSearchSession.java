package scit.ainiinu.lostpet.domain;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "lost_pet_search_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostPetSearchSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_pet_id", nullable = false)
    private LostPetReport lostPetReport;

    @Column(name = "query_mode", nullable = false, length = 20)
    private String queryMode;

    @Column(name = "query_image_url", length = 1000)
    private String queryImageUrl;

    @Column(name = "query_text", length = 2000)
    private String queryText;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private LostPetSearchSession(
            Long ownerId,
            LostPetReport lostPetReport,
            String queryMode,
            String queryImageUrl,
            String queryText,
            LocalDateTime expiresAt
    ) {
        this.ownerId = ownerId;
        this.lostPetReport = lostPetReport;
        this.queryMode = queryMode;
        this.queryImageUrl = queryImageUrl;
        this.queryText = queryText;
        this.expiresAt = expiresAt;
    }

    public static LostPetSearchSession create(
            Long ownerId,
            LostPetReport lostPetReport,
            String queryMode,
            String queryImageUrl,
            String queryText,
            LocalDateTime expiresAt
    ) {
        return new LostPetSearchSession(ownerId, lostPetReport, queryMode, queryImageUrl, queryText, expiresAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
