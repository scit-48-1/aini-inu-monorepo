package scit.ainiinu.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.Gender;
import scit.ainiinu.member.entity.enums.MemberStatus;
import scit.ainiinu.member.entity.enums.MemberType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MemberResponse {
    private Long id;
    private String email;
    private String nickname;
    private MemberType memberType;
    private String profileImageUrl;
    private String linkedNickname;
    private String phone;
    private Integer age;
    private Gender gender;
    private String mbti;
    private String personality;
    private String selfIntroduction;
    
    private List<MemberPersonalityTypeResponse> personalityTypes;
    
    private BigDecimal mannerTemperature;
    private MemberStatus status;
    
    @JsonProperty("isVerified")
    private boolean isVerified;
    
    private LocalDateTime createdAt;
    private LocalDateTime nicknameChangedAt;

    public static MemberResponse from(Member member, List<MemberPersonalityTypeResponse> personalityTypes) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .memberType(member.getMemberType())
                .profileImageUrl(member.getProfileImageUrl())
                .linkedNickname(member.getLinkedNickname())
                .phone(member.getPhone())
                .age(member.getAge())
                .gender(member.getGender())
                .mbti(member.getMbti())
                .personality(member.getPersonality())
                .selfIntroduction(member.getSelfIntroduction())
                .personalityTypes(personalityTypes)
                .mannerTemperature(member.getMannerTemperature() != null
                        ? member.getMannerTemperature().getValue()
                        : null)
                .status(member.getStatus())
                .isVerified(member.isVerified())
                .createdAt(member.getCreatedAt())
                .nicknameChangedAt(member.getNicknameChangedAt())
                .build();
    }
}
