package scit.ainiinu.member.dto.response;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.member.entity.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MemberFollowResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private BigDecimal mannerTemperature;
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
