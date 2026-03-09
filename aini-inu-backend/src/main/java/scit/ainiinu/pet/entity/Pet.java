package scit.ainiinu.pet.entity;

import jakarta.persistence.*;

import lombok.AccessLevel;

import lombok.Builder;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;

import scit.ainiinu.common.entity.BaseTimeEntity;

import scit.ainiinu.pet.entity.enums.PetGender;

import scit.ainiinu.pet.entity.enums.PetSize;



@Entity

@Getter

@NoArgsConstructor(access = AccessLevel.PROTECTED)

@AllArgsConstructor(access = AccessLevel.PRIVATE)

@Builder

public class Pet extends BaseTimeEntity {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    

    @Column(nullable = false)

    private Long memberId; // Member Context 참조 (ID only)

    

    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "breed_id")

    private Breed breed;

    

    @Column(nullable = false, length = 10)

    private String name;



    @Column(nullable = false)

    private Integer age;



    @Enumerated(EnumType.STRING)

    @Column(nullable = false, length = 10)

    private PetGender gender;



    @Enumerated(EnumType.STRING)

    @Column(nullable = false, length = 10)

    private PetSize size;



    @Column(length = 4)

    private String mbti;



    @Column(nullable = false)

    private Boolean isNeutered;



    private String photoUrl;



    @Column(nullable = false)

    private Boolean isMain;




    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)

    @Builder.Default

    private java.util.List<PetPersonality> petPersonalities = new java.util.ArrayList<>();



    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)

    @Builder.Default

    private java.util.List<PetWalkingStyle> petWalkingStyles = new java.util.ArrayList<>();



    public void addPersonality(Personality personality) {

        PetPersonality petPersonality = new PetPersonality(this, personality);

        this.petPersonalities.add(petPersonality);

    }



    public void addWalkingStyle(WalkingStyle walkingStyle) {

        PetWalkingStyle petWalkingStyle = new PetWalkingStyle(this, walkingStyle);

        this.petWalkingStyles.add(petWalkingStyle);

    }

    public void updateBasicInfo(String name, Integer age, Boolean isNeutered, String mbti, String photoUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (age != null) {
            this.age = age;
        }
        if (isNeutered != null) {
            this.isNeutered = isNeutered;
        }
        if (mbti != null) {
            this.mbti = mbti;
        }
        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
    }

    public void clearPersonalities() {
        this.petPersonalities.clear();
    }

    public void clearWalkingStyles() {
        this.petWalkingStyles.clear();
    }

    public void setMain(boolean isMain) {

        this.isMain = isMain;

    }

}
