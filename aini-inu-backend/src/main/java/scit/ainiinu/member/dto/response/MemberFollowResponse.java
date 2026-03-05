package scit.ainiinu.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.member.entity.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MemberFollowResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "닉네임입니다.", example = "몽이아빠")
    private String nickname;
    @Schema(description = "프로필 이미지 URL입니다.", example = "https://cdn.example.com/sample.jpg")
    private String profileImageUrl;
    @Schema(description = "mannerTemperature 값입니다.", example = "4.5")
    private BigDecimal mannerTemperature;
    @Schema(description = "followedAt 값입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime followedAt;

    public static MemberFollowResponse of(Member member, LocalDateTime followedAt) {
        return MemberFollowResponse.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .mannerTemperature(member.getMannerTemperature() != null
                        ? member.getMannerTemperature().getValue()
                        : null)
                .followedAt(followedAt)
                .build();
    }
}
