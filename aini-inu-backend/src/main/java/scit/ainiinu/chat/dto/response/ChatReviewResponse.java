package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatReviewResponse {
    private Long id;
    private Long chatRoomId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
