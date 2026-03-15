package scit.ainiinu.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardRecentFriendResponse {

    @Schema(description = "친구 회원 ID", example = "12")
    private Long memberId;

    @Schema(description = "채팅방 ID", example = "101")
    private Long chatRoomId;

    @Schema(description = "대시보드 표시 이름", example = "초코, 몽이")
    private String displayName;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "표시용 점수", example = "7.0")
    private double score;

    @JsonIgnore
    public Long getRoomId() {
        return chatRoomId;
    }
}
