package scit.ainiinu.pet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetWalkingStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "walking_style_id")
    private WalkingStyle walkingStyle;

    public PetWalkingStyle(Pet pet, WalkingStyle walkingStyle) {
        this.pet = pet;
        this.walkingStyle = walkingStyle;
    }
}
