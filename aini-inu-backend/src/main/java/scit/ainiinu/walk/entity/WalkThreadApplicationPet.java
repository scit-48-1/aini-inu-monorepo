package scit.ainiinu.walk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "thread_application_pet",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"application_id", "pet_id"})
        }
)
public class WalkThreadApplicationPet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    private WalkThreadApplicationPet(Long applicationId, Long petId) {
        this.applicationId = applicationId;
        this.petId = petId;
    }

    public static WalkThreadApplicationPet of(Long applicationId, Long petId) {
        return new WalkThreadApplicationPet(applicationId, petId);
    }
}
