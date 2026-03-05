package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "팔로잉 대상 회원 단위 스토리 그룹 응답")
public class StoryGroupResponse {
    @Schema(description = "스토리 작성자(산책일기 작성자) 회원 ID", example = "7")
    private Long memberId;

    @Schema(description = "작성자 닉네임", example = "몽이아빠")
    private String nickname;

    @Schema(description = "작성자 프로필 이미지 URL", example = "https://cdn.example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "스토리 그룹 커버 이미지 URL(최신 산책일기 첫 이미지)", example = "https://cdn.example.com/diary-cover.jpg")
    private String coverImageUrl;

    @Schema(description = "그룹 내 최신 산책일기 작성 시각(UTC)", example = "2026-03-04T01:30:00Z")
    private OffsetDateTime latestCreatedAt;

    @Schema(description = "24시간 내 노출 대상 산책일기 목록(최신순)")
    private List<StoryDiaryItemResponse> diaries;
}
