package scit.ainiinu.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "무한 스크롤용 Slice 응답")
public class SliceResponse<T> {
    @Schema(description = "조회된 항목 목록")
    private List<T> content;
    @Schema(description = "현재 페이지 번호 (0-base)", example = "0")
    private int pageNumber;
    @Schema(description = "페이지 크기", example = "20")
    private int pageSize;
    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;
    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    public static <T> SliceResponse<T> of(Slice<T> slice) {
        return new SliceResponse<>(
            slice.getContent(),
            slice.getNumber(),
            slice.getSize(),
            slice.isFirst(),
            slice.isLast(),
            slice.hasNext()
        );
    }
}
