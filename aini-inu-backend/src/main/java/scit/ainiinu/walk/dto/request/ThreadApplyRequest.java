package scit.ainiinu.walk.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ThreadApplyRequest {
    @Schema(description = "petIds 값입니다.", example = "[101,102]")
    private List<Long> petIds;
}
