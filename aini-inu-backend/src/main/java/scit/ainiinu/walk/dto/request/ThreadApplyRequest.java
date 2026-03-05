package scit.ainiinu.walk.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ThreadApplyRequest {
    private List<Long> petIds;
}
