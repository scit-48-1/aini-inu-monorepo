package scit.ainiinu.community.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostLikeResponse {
    private boolean isLiked;
    private int likeCount;
}
