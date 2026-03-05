package scit.ainiinu.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "커서 기반 페이징 응답")
public class CursorResponse<T> {
    @Schema(description = "조회된 항목 목록")
    private List<T> content;
    @Schema(description = "다음 조회용 커서", example = "2701")
    private String nextCursor;
    @Schema(description = "추가 데이터 존재 여부", example = "true")
    private boolean hasMore;
}
