package scit.ainiinu.walk.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ThreadHotspotResponse {
    private String region;
    private Long count;
}
