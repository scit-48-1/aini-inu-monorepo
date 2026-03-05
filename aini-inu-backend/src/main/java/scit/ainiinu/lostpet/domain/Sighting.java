package scit.ainiinu.lostpet.domain;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "sighting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sighting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finder_id", nullable = false)
    private Long finderId;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    @Column(name = "found_at", nullable = false)
    private LocalDateTime foundAt;

    @Column(name = "found_location", nullable = false, length = 255)
    private String foundLocation;

    @Column(name = "memo", length = 2000)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SightingStatus status;

    private Sighting(
            Long finderId,
            String photoUrl,
            LocalDateTime foundAt,
            String foundLocation,
            String memo
    ) {
        this.finderId = finderId;
        this.photoUrl = photoUrl;
        this.foundAt = foundAt;
        this.foundLocation = foundLocation;
        this.memo = memo;
        this.status = SightingStatus.OPEN;
    }

    public static Sighting create(
            Long finderId,
            String photoUrl,
            LocalDateTime foundAt,
            String foundLocation,
            String memo
    ) {
        return new Sighting(finderId, photoUrl, foundAt, foundLocation, memo);
    }

    public void close() {
        this.status = SightingStatus.CLOSED;
    }

    public void assignIdForTest(Long id) {
        this.id = id;
    }
}
