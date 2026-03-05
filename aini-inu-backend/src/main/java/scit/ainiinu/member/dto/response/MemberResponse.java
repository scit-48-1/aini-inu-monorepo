package scit.ainiinu.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "이메일 주소입니다.", example = "user@example.com")
    private String email;
    @Schema(description = "닉네임입니다.", example = "몽이아빠")
    private String nickname;
    @Schema(
            description = "회원 유형 코드입니다.",
            example = "PET_OWNER",
            allowableValues = {"PET_OWNER", "NON_PET_OWNER", "ADMIN"}
    )
    private MemberType memberType;
    @Schema(description = "프로필 이미지 URL입니다.", example = "https://cdn.example.com/sample.jpg")
    private String profileImageUrl;
    @Schema(description = "연동 계정 표시 닉네임입니다.", example = "몽이아빠")
    private String linkedNickname;
    @Schema(description = "전화번호입니다.", example = "01012345678")
    private String phone;
    @Schema(description = "나이입니다.", example = "20")
    private Integer age;
    @Schema(description = "성별 코드입니다.", example = "UNKNOWN", allowableValues = {"MALE", "FEMALE", "UNKNOWN"})
    private Gender gender;
    @Schema(description = "MBTI 문자열입니다.", example = "예시 문자열")
    private String mbti;
    @Schema(description = "성격 설명 문자열입니다.", example = "예시 문자열")
    private String personality;
    @Schema(description = "자기소개 문구입니다.", example = "예시 문자열")
    private String selfIntroduction;
    
    @Schema(description = "회원 성향 타입 상세 목록입니다.", example = "[\"예시 항목\"]")
    private List<MemberPersonalityTypeResponse> personalityTypes;

    @Schema(description = "매너 온도 점수입니다.", example = "36.5")
    private BigDecimal mannerTemperature;
    @Schema(description = "상태 코드입니다.", example = "ACTIVE")
    private MemberStatus status;
    
    @JsonProperty("isVerified")
    @Schema(description = "인증 여부입니다.", example = "true")
    private boolean isVerified;
    
    @Schema(description = "생성 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime createdAt;
    @Schema(description = "닉네임 최근 변경 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
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
