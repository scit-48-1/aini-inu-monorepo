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
        name = "thread_pet",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"thread_id", "pet_id"})
        }
)
public class WalkThreadPet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Column(name = "pet_id", nullable = false)
    private Long petId;

    private WalkThreadPet(Long threadId, Long petId) {
        this.threadId = threadId;
        this.petId = petId;
    }

    public static WalkThreadPet of(Long threadId, Long petId) {
        return new WalkThreadPet(threadId, petId);
    }
}
