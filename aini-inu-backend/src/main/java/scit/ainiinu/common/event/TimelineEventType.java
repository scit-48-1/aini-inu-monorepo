package scit.ainiinu.common.event;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타임라인 이벤트 타입", enumAsRef = true)
public enum TimelineEventType {

    @Schema(description = "산책 모집글 생성")
    WALK_THREAD_CREATED,

    @Schema(description = "실시간 산책 시작")
    WALKING_SESSION_STARTED,

    @Schema(description = "실시간 산책 완료")
    WALKING_SESSION_COMPLETED,

    @Schema(description = "커뮤니티 게시글 작성")
    POST_CREATED,

    @Schema(description = "반려동물 실종 신고 등록")
    LOST_PET_REPORT_CREATED,

    @Schema(description = "실종 반려동물 목격 제보 등록")
    SIGHTING_CREATED,

    @Schema(description = "산책 일기 작성")
    WALK_DIARY_CREATED
}
