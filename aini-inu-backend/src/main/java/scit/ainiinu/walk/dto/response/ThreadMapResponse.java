package scit.ainiinu.walk.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ThreadMapResponse {
    private Long threadId;
    private String title;
    private String chatType;
    private Integer currentParticipants;
    private Integer maxParticipants;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String placeName;
}
