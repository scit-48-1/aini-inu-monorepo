package scit.ainiinu.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowStatusResponse {

    @JsonProperty("isFollowing")
    private boolean isFollowing;
}
