package scit.ainiinu.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowStatusResponse {

    @JsonProperty("isFollowing")
    @Schema(description = "isFollowing 값입니다.", example = "true")
    private boolean isFollowing;
}
