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
@Table(name = "lost_pet_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostPetReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "pet_name", nullable = false, length = 100)
    private String petName;

    @Column(name = "breed", length = 100)
    private String breed;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "last_seen_location", nullable = false, length = 255)
    private String lastSeenLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LostPetReportStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    private LostPetReport(
            Long ownerId,
            String petName,
            String breed,
            String photoUrl,
            String description,
            LocalDateTime lastSeenAt,
            String lastSeenLocation
    ) {
        this.ownerId = ownerId;
        this.petName = petName;
        this.breed = breed;
        this.photoUrl = photoUrl;
        this.description = description;
        this.lastSeenAt = lastSeenAt;
        this.lastSeenLocation = lastSeenLocation;
        this.status = LostPetReportStatus.ACTIVE;
    }

    public static LostPetReport create(
            Long ownerId,
            String petName,
            String breed,
            String photoUrl,
            String description,
            LocalDateTime lastSeenAt,
            String lastSeenLocation
    ) {
        return new LostPetReport(ownerId, petName, breed, photoUrl, description, lastSeenAt, lastSeenLocation);
    }

    public void resolve() {
        this.status = LostPetReportStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void close() {
        this.status = LostPetReportStatus.CLOSED;
    }

    // 테스트에서 ID 세팅이 필요할 때만 사용
    public void assignIdForTest(Long id) {
        this.id = id;
    }
}
