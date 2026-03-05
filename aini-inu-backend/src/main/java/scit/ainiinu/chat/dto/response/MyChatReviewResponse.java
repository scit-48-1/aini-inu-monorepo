package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyChatReviewResponse {
    private boolean exists;
    private ChatReviewResponse review;
}
