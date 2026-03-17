package scit.ainiinu.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import scit.ainiinu.common.entity.BaseTimeEntity;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;
import scit.ainiinu.member.entity.enums.Gender;
import scit.ainiinu.member.entity.enums.MemberStatus;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.entity.vo.MannerTemperature;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 10, unique = true)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberType memberType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "linked_nickname")
    private String linkedNickname;

    @Column(length = 13)
    private String phone;

    @Column(name = "nickname_changed_at")
    private LocalDateTime nicknameChangedAt;

    private Integer age;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(length = 4)
    private String mbti;

    private String personality;
    private String selfIntroduction;

    @Column(nullable = false)
    private boolean isVerified;

    @Column(name = "is_timeline_public", nullable = false)
    private boolean isTimelinePublic = true;

    @Embedded
    private MannerTemperature mannerTemperature;

    private int mannerScoreSum;
    private int mannerScoreCount;

    @Builder
    public Member(String email, String password, String nickname, String profileImageUrl, MemberType memberType,
                  String phone, Integer age, Gender gender, String mbti, String personality, String selfIntroduction) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.memberType = memberType != null ? memberType : MemberType.NON_PET_OWNER;
        this.status = MemberStatus.ACTIVE;
        this.phone = phone;
        this.age = age;
        this.gender = gender;
        this.mbti = mbti;
        this.personality = personality;
        this.selfIntroduction = selfIntroduction;
        this.isVerified = false;
        this.isTimelinePublic = true;
        this.mannerTemperature = new MannerTemperature(new java.math.BigDecimal("5.0"));
        this.mannerScoreSum = 0;
        this.mannerScoreCount = 0;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void addMannerScore(int score) {
        if (score < 1 || score > 10) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        this.mannerScoreSum += score;
        this.mannerScoreCount += 1;
        this.mannerTemperature = MannerTemperature.fromAverage(this.mannerScoreSum, this.mannerScoreCount);
    }

    public void updateProfile(String nickname, String profileImageUrl, String linkedNickname, String phone,
                              Integer age, Gender gender, String mbti, String personality, String selfIntroduction) {
        if (nickname != null && !nickname.isBlank() && !nickname.equals(this.nickname)) {
            this.nickname = nickname;
            this.nicknameChangedAt = LocalDateTime.now();
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (linkedNickname != null) {
            this.linkedNickname = linkedNickname;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (age != null) {
            this.age = age;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (mbti != null) {
            this.mbti = mbti;
        }
        if (personality != null) {
            this.personality = personality;
        }
        if (selfIntroduction != null) {
            this.selfIntroduction = selfIntroduction;
        }
    }

    public void verify() {
        this.isVerified = true;
    }

    public void upgradeToPetOwner() {
        this.memberType = MemberType.PET_OWNER;
    }

    public void downgradeToNonPetOwner() {
        this.memberType = MemberType.NON_PET_OWNER;
    }

    public void updateTimelinePublic(boolean isTimelinePublic) {
        this.isTimelinePublic = isTimelinePublic;
    }
}
